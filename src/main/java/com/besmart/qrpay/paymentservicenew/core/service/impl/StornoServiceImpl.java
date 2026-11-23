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
import com.besmart.qrpay.paymentservicenew.core.service.StornoService;
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
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StornoServiceImpl implements StornoService {

    private final TxRunner tx;
    private final PaymentRepository payments;
    private final PaymentStore store;
    private final PaymentDbMapper dbMapper;
    private final SouClient souClient;
    private final PaymentNetMapper netMapper;
    private final Clock clock;

    /**
     * Cash-desk side cancellation. If the payment has not reached this service yet,
     * a STORNO_PENDING row is persisted and the SOU call is deferred until the payment
     * arrives. This replaces the five minute Redis cache used by cash-desk-api.
     */
    @Override
    public OperationResult storno(PaymentCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        Payment storno = createOrGet(command);

        if (storno.getState() == PaymentState.STORNO_PENDING) {
            return OperationResult.error(ApplicationError.SALE_TRANSACTION_NOT_FOUND, now);
        }
        if (storno.isFinal()) {
            return OperationResult.stored(storno, store.receipts(storno.getId()), now);
        }
        return execute(storno.getId(), command, now);
    }

    /**
     * Called after a successful payment. Returns true if the payment was cancelled by a
     * storno that arrived earlier.
     */
    @Override
    public boolean executePendingAfterPayment(String terminalId, String trxId, Long retailId, String timeZoneId) {
        try {
            return Boolean.TRUE.equals(tx.inTx(() -> {
                Optional<PaymentEntity> found = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                        terminalId, TrxUtils.withSuffix(trxId, "-S"), PaymentType.STORNO);
                if (found.isEmpty()) {
                    return false;
                }
                Payment pending = dbMapper.toDomain(found.get());
                if (isCompleted(pending)) {
                    return true;
                }
                if (pending.getState() == PaymentState.LEGACY) {
                    // Never retry a row produced by the old services: it may already
                    // have been accepted by SOU.
                    return false;
                }
                Payment locked = payments.findByIdForUpdate(pending.getId())
                        .map(dbMapper::toDomain)
                        .orElseThrow();
                PaymentCommand synthetic = new PaymentCommand(
                        terminalId,
                        retailId == null ? 0L : retailId,
                        trxId,
                        locked.getShiftNumber() == null ? 0 : locked.getShiftNumber(),
                        null,
                        locked.getReceiptNumber() == null ? null : Integer.valueOf(locked.getReceiptNumber()),
                        locked.money(),
                        locked.getQrPayClientId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        timeZoneId,
                        locked.getTerminalIp(),
                        null,
                        null,
                        null,
                        null,
                        null
                );
                return doExecute(locked, synthetic) != null;
            }));
        } catch (Exception ex) {
            // A delayed storno must not turn a successful payment into an error.
            log.error("Delayed storno failed for terminal {} trx {}", terminalId, trxId, ex);
            return false;
        }
    }

    @Override
    public boolean isCompleted(String terminalId, String trxId) {
        return payments.findByCashDeskUidAndTrnIdAndPaymentType(
                        terminalId, TrxUtils.withSuffix(trxId, "-S"), PaymentType.STORNO)
                .map(dbMapper::toDomain)
                .map(this::isCompleted)
                .orElse(false);
    }

    private Payment createOrGet(PaymentCommand command) {
        return tx.inTx(() -> {
            payments.acquireTransactionLock(store.lockKey(command.terminalId(), command.trxId()));
            String stornoTrx = TrxUtils.withSuffix(command.trxId(), "-S");
            Optional<PaymentEntity> existing = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                    command.terminalId(), stornoTrx, PaymentType.STORNO);
            if (existing.isPresent()) {
                return dbMapper.toDomain(existing.get());
            }

            Optional<Payment> sale = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                            command.terminalId(), command.trxId(), PaymentType.SALE)
                    .map(dbMapper::toDomain);
            Payment storno = new Payment();
            storno.setCashDeskUid(command.terminalId());
            storno.setTrnId(stornoTrx);
            storno.setPaymentDate(TimeZones.toUtc(
                    TimeZones.nowInZone(clock, command.timeZoneId()), command.timeZoneId()));
            storno.setPaymentType(PaymentType.STORNO);
            storno.setSuccess(false);
            storno.setShiftNumber(command.shiftNo());
            storno.setReceiptNumber(command.receiptNumber() == null
                    ? null : String.valueOf(command.receiptNumber()));
            storno.setTerminalIp(command.terminalIp());
            storno.setState(sale.isPresent() ? PaymentState.CREATED : PaymentState.STORNO_PENDING);
            applySaleOrCommand(storno, sale.orElse(null), command);

            PaymentEntity saved = payments.save(dbMapper.toEntity(storno));
            store.saveReceipts(saved.getId(), command.receipts(), storno.money(), command.trxId());
            return dbMapper.toDomain(saved);
        });
    }

    private OperationResult execute(Long id, PaymentCommand command, LocalDateTime now) {
        return tx.inTx(() -> {
            Payment storno = payments.findByIdForUpdate(id)
                    .map(dbMapper::toDomain)
                    .orElseThrow();
            Collection<Receipt> receipts = store.receipts(id);

            if (storno.isFinal()) {
                return OperationResult.stored(storno, receipts, now);
            }

            SouOutcome sou = doExecute(storno, command);
            if (sou == null) {
                return OperationResult.error(ApplicationError.SALE_TRANSACTION_NOT_FOUND, now);
            }
            return OperationResult.of(storno, receipts, sou, now);
        });
    }

    /**
     * Runs with the storno row already locked in the current transaction. Returns the
     * SOU outcome, or {@code null} when the sale has not succeeded yet.
     */
    private SouOutcome doExecute(Payment storno, PaymentCommand command) {
        Optional<Payment> saleOpt = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                        storno.getCashDeskUid(), TrxUtils.clearTrx(storno.getTrnId()), PaymentType.SALE)
                .map(dbMapper::toDomain);
        if (saleOpt.isEmpty() || !saleOpt.get().isSuccess()) {
            storno.setState(PaymentState.STORNO_PENDING);
            payments.save(dbMapper.toEntity(storno));
            return null;
        }

        Payment sale = saleOpt.get();
        // Existing cash-desk behaviour: the storno amount always follows the sale.
        storno.applyMoney(sale.money());
        storno.setQrPayClientId(sale.getQrPayClientId());

        SouOutcome sou;
        try {
            SouPaymentResponse response = souClient.storno(netMapper.toNet(storno, command));
            sou = netMapper.toDomain(response);
        } catch (FeignException ex) {
            throw new SouUnavailableException("SOU storno call failed for trx " + storno.getTrnId(), ex);
        }

        if (sou.success()) {
            storno.complete(sou.souTransactionId(), paymentDate(sou, command));
            sale.setStorno(true);
            payments.save(dbMapper.toEntity(sale));
        } else {
            storno.decline(sou.errorMessage());
        }
        payments.save(dbMapper.toEntity(storno));
        return sou;
    }

    private void applySaleOrCommand(Payment storno, Payment sale, PaymentCommand command) {
        if (sale != null) {
            storno.applyMoney(sale.money());
            storno.setQrPayClientId(sale.getQrPayClientId());
        } else {
            storno.applyMoney(command.money());
            storno.setQrPayClientId(command.qrPayClientId());
        }
    }

    private LocalDateTime paymentDate(SouOutcome sou, PaymentCommand command) {
        LocalDateTime souTime = sou.operationTime();
        return souTime == null ? null : TimeZones.toUtc(souTime, command.timeZoneId());
    }

    private boolean isCompleted(Payment storno) {
        return storno.getState() == PaymentState.COMPLETED
                || (storno.getState() == PaymentState.LEGACY && storno.isSuccess());
    }
}
