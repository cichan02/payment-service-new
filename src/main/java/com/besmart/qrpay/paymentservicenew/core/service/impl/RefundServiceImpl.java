package com.besmart.qrpay.paymentservicenew.core.service.impl;

import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;.ApplicationError;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.BusinessException;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.SouUnavailableException;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;
import com.besmart.qrpay.paymentservicenew.core.domain.model.PaymentC;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;
import com.besmart.qrpay.paymentservicenew.core.domain.model.SouOutcome;
import com.besmart.qrpay.paymentservicenew.core.domain.result.OperationResult;
import com.besmart.qrpay.paymentservicenew.core.service.RefundService;
import com.besmart.qrpay.paymentservicenew.core.service.TxRunner;
import com.besmart.qrpay.paymentservicenew.core.util.TimeZones;
import com.besmart.qrpay.paymentservicenew.core.util.TrxUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final TxRunner tx;
    private final PaymentRepository payments;
    private final PaymentStore store;
    private final PaymentDbMapper dbMapper;
    private final SouClient souClient;
    private final PaymentNetMapper netMapper;
    private final Clock clock;

    /**
     * Customer side cancellation. The refund is a new transaction stored with the
     * {@code -R} suffix; {@code originalSouTransactionId} points at the payment being
     * refunded.
     */
    @Override
    public OperationResult refund(PaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Payment refund = createOrGet(command);

        if (refund.isFinal()) {
            return OperationResult.stored(refund, store.receipts(refund.getId()), now);
        }
        return execute(refund.getId(), command, now);
    }

    private Payment createOrGet(PaymentCommand command) {
        return tx.inTx(() -> {
            payments.acquireTransactionLock(store.lockKey(command.terminalId(), command.trxId()));
            String refundTrx = TrxUtils.withSuffix(command.trxId(), "-R");
            Optional<PaymentEntity> existing = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                    command.terminalId(), refundTrx, PaymentType.REFUND);
            if (existing.isPresent()) {
                Payment refund = dbMapper.toDomain(existing.get());
                store.assertSameTransaction(refund, command.money());
                return refund;
            }

            Payment original = payments.findFirstBySouIdOrderByIdAsc(command.originalSouTransactionId())
                    .map(dbMapper::toDomain)
                    .orElseThrow(() -> new BusinessException(ApplicationError.ORIGINAL_PAYMENT_NOT_FOUND,
                            HttpStatus.NOT_FOUND, "No payment with souId " + command.originalSouTransactionId()));

            Payment refund = new Payment();
            refund.setCashDeskUid(command.terminalId());
            refund.setTrnId(refundTrx);
            refund.setPaymentDate(TimeZones.toUtc(
                    TimeZones.nowInZone(clock, command.timeZoneId()), command.timeZoneId()));
            refund.setPaymentType(PaymentType.REFUND);
            refund.setSuccess(false);
            refund.setState(PaymentState.CREATED);
            refund.applyMoney(command.money());
            refund.setShiftNumber(command.shiftNo());
            refund.setReceiptNumber(command.receiptNumber() == null
                    ? null : String.valueOf(command.receiptNumber()));
            refund.setTerminalIp(command.terminalIp());
            // Existing behaviour: the refund inherits the client and pay tool from the
            // payment being refunded, so it is visible in the client's history.
            refund.setQrPayClientId(original.getQrPayClientId());
            refund.setPayToolId(original.getPayToolId());

            PaymentEntity saved = payments.save(dbMapper.toEntity(refund));
            store.saveReceipts(saved.getId(), command.receipts(), command.money(), command.trxId());
            return dbMapper.toDomain(saved);
        });
    }

    private OperationResult execute(Long id, PaymentCommand command, LocalDateTime now) {
        return tx.inTx(() -> {
            PaymentEntity entity = payments.findByIdForUpdate(id).orElseThrow();
            Payment refund = dbMapper.toDomain(entity);
            Collection<Receipt> receipts = store.receipts(id);
            if (refund.isFinal()) {
                return OperationResult.stored(refund, receipts, now);
            }

            SouOutcome sou;
            try {
                SouPaymentResponse response = souClient.refund(
                        netMapper.toNet(refund, command, receipts, PaymentType.REFUND));
                sou = netMapper.toDomain(response);
            } catch (FeignException ex) {
                throw new SouUnavailableException("SOU refund call failed for trx " + refund.getTrnId(), ex);
            }

            if (sou.success()) {
                refund.complete(sou.souTransactionId(), paymentDate(sou, command));
            } else {
                refund.decline(sou.errorMessage());
            }
            payments.save(dbMapper.toEntity(refund));
            return OperationResult.of(refund, receipts, sou, now);
        });
    }

    private LocalDateTime paymentDate(SouOutcome sou, PaymentCommand command) {
        LocalDateTime souTime = sou.operationTime();
        return souTime == null ? null : TimeZones.toUtc(souTime, command.timeZoneId());
    }
}
