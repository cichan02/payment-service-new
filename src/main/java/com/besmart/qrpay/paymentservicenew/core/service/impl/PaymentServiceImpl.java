package com.besmart.qrpay.paymentservicenew.core.service.impl;

import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;.ApplicationError;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.SouUnavailableException;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;
import com.besmart.qrpay.paymentservicenew.core.domain.model.PaymentC;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;
import com.besmart.qrpay.paymentservicenew.core.domain.model.SouOutcome;
import com.besmart.qrpay.paymentservicenew.core.domain.result.OperationResult;
import com.besmart.qrpay.paymentservicenew.core.service.PaymentService;
import com.besmart.qrpay.paymentservicenew.core.service.StornoService;
import com.besmart.qrpay.paymentservicenew.core.service.TxRunner;
import com.besmart.qrpay.paymentservicenew.core.util.TimeZones;
import com.besmart.qrpay.paymentservicenew.infra.network.client.SouClient;
import com.besmart.qrpay.paymentservicenew.infra.network.mapper.PaymentNetMapper;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouPaymentResponse;
import com.besmart.qrpay.paymentservicenew.infra.persistence.PaymentStore;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.mapper.PaymentDbMapper;
import com.besmart.qrpay.paymentservicenew.infra.persistence.repository.PaymentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final TxRunner tx;
    private final PaymentRepository payments;
    private final PaymentStore store;
    private final PaymentDbMapper dbMapper;
    private final SouClient souClient;
    private final PaymentNetMapper netMapper;
    private final StornoService stornoService;
    private final Clock clock;

    /**
     * Payment operation. Two transactions: the intent row is committed first
     * (state CREATED), then SOU is called under a row lock. A crash between the two
     * leaves a durable CREATED row and the same request can be retried.
     */
    @Override
    public OperationResult pay(PaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Payment payment = createOrGet(command);
        Collection<Receipt> receipts = store.receipts(payment.getId());

        if (payment.isFinal()) {
            return cancelledOrStored(command, payment, receipts, now);
        }

        Execution execution = tx.inTx(() -> execute(payment.getId(), command));
        Collection<Receipt> resultReceipts = store.receipts(execution.payment().getId());

        if (execution.sou() == null) {
            return cancelledOrStored(command, execution.payment(), resultReceipts, now);
        }
        if (execution.sou().success()
                && stornoService.executePendingAfterPayment(command.terminalId(), command.trxId(),
                command.retailId(), command.timeZoneId())) {
            log.info("Payment {} was cancelled by a delayed storno", command.trxId());
            return OperationResult.error(ApplicationError.PAYMENT_CANCELLED, now);
        }
        return OperationResult.of(execution.payment(), resultReceipts, execution.sou(), now);
    }

    private OperationResult cancelledOrStored(PaymentCommand command, Payment payment,
                                              Collection<Receipt> receipts, LocalDateTime now) {
        // A duplicate request is also the recovery path for a delayed storno whose SOU
        // call failed earlier: the payment is already final, so nothing else would
        // retry it.
        if (payment.isSuccess()) {
            stornoService.executePendingAfterPayment(command.terminalId(), command.trxId(),
                    command.retailId(), command.timeZoneId());
        }
        if (stornoService.isCompleted(command.terminalId(), command.trxId())) {
            return OperationResult.error(ApplicationError.PAYMENT_CANCELLED, now);
        }
        return OperationResult.stored(payment, receipts, now);
    }

    private Payment createOrGet(PaymentCommand command) {
        return tx.inTx(() -> {
            payments.acquireTransactionLock(store.lockKey(command.terminalId(), command.trxId()));
            Optional<PaymentEntity> existing = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                    command.terminalId(), command.trxId(), PaymentType.SALE);
            if (existing.isPresent()) {
                Payment payment = dbMapper.toDomain(existing.get());
                store.assertSameTransaction(payment, command.money());
                return payment;
            }

            Payment payment = new Payment();
            payment.setCashDeskUid(command.terminalId());
            payment.setTrnId(command.trxId());
            payment.setPaymentDate(TimeZones.toUtc(
                    TimeZones.nowInZone(clock, command.timeZoneId()), command.timeZoneId()));
            payment.applyMoney(command.money());
            payment.setShiftNumber(command.shiftNo());
            payment.setReceiptNumber(String.valueOf(command.receiptNumber()));
            payment.setQrPayClientId(command.qrPayClientId());
            payment.setPayToolId(command.payToolId());
            payment.setSmartPaymentCardNumber(command.smartPaymentCardNumber());
            payment.setTerminalIp(command.terminalIp());
            payment.setPaymentType(PaymentType.SALE);
            payment.setSuccess(false);
            payment.setState(PaymentState.CREATED);

            PaymentEntity saved = payments.save(dbMapper.toEntity(payment));
            store.saveReceipts(saved.getId(), command.receipts(), command.money(), command.trxId());
            return dbMapper.toDomain(saved);
        });
    }

    private Execution execute(Long id, PaymentCommand command) {
        return tx.inTx(() -> {
            PaymentEntity entity = payments.findByIdForUpdate(id).orElseThrow();
            Payment payment = dbMapper.toDomain(entity);
            if (payment.isFinal()) {
                return new Execution(payment, null);
            }
            Collection<Receipt> receipts = store.receipts(id);

            SouOutcome sou;
            try {
                SouPaymentResponse response = souClient.payment(
                        netMapper.toNet(payment, command, receipts, PaymentType.SALE));
                sou = netMapper.toDomain(response);
            } catch (FeignException ex) {
                throw new SouUnavailableException("SOU payment call failed for trx " + payment.getTrnId(), ex);
            }

            if (sou.success()) {
                payment.complete(sou.souTransactionId(), paymentDate(sou, command));
            } else {
                payment.decline(sou.errorMessage());
            }
            payments.save(dbMapper.toEntity(payment));
            return new Execution(payment, sou);
        });
    }

    private LocalDateTime paymentDate(SouOutcome sou, PaymentCommand command) {
        LocalDateTime souTime = sou.operationTime();
        return souTime == null ? null : TimeZones.toUtc(souTime, command.timeZoneId());
    }

    private record Execution(Payment payment, SouOutcome sou) {
    }
}
