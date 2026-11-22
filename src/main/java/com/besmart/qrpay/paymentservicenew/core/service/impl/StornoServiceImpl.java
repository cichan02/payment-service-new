package com.besmart.qrpay.paymentservicenew.core.service.impl;

import com.besmart.qrpay.paymentservicenew.api.rest.mapper.ResponseFactory;
import com.besmart.qrpay.paymentservicenew.api.rest.to.common.MoneyDto;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.StornoRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.ApplicationError;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.SouUnavailableException;
import com.besmart.qrpay.paymentservicenew.core.service.StornoService;
import com.besmart.qrpay.paymentservicenew.core.service.TxRunner;
import com.besmart.qrpay.paymentservicenew.core.util.TimeZones;
import com.besmart.qrpay.paymentservicenew.core.util.TrxUtils;
import com.besmart.qrpay.paymentservicenew.infra.network.client.SouClient;
import com.besmart.qrpay.paymentservicenew.infra.network.mapper.SouRequestFactory;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouPaymentResponse;
import com.besmart.qrpay.paymentservicenew.infra.persistence.PaymentStore;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.ReceiptEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.repository.PaymentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StornoServiceImpl implements StornoService {

    private final TxRunner tx;
    private final PaymentRepository payments;
    private final PaymentStore store;
    private final SouClient souClient;
    private final SouRequestFactory souFactory;
    private final ResponseFactory responseFactory;
    private final Clock clock;

    /**
     * Cash-desk side cancellation. If the payment has not reached this service yet,
     * a STORNO_PENDING row is persisted and the SOU call is deferred until the payment
     * arrives. This replaces the five minute Redis cache used by cash-desk-api.
     */
    @Override
    public OperationResponse storno(StornoRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        PaymentEntity storno = createOrGet(request);

        if (storno.getState() == PaymentState.STORNO_PENDING) {
            return responseFactory.error(ApplicationError.SALE_TRANSACTION_NOT_FOUND,
                    ApplicationError.SALE_TRANSACTION_NOT_FOUND.getMessage(), now);
        }
        if (isFinal(storno)) {
            return responseFactory.stored(storno, store.receipts(storno.getId()), now);
        }
        return execute(storno.getId(), request, now);
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
                PaymentEntity storno = found.get();
                if (isCompleted(storno)) {
                    return true;
                }
                if (storno.getState() == PaymentState.LEGACY) {
                    // Never retry a row produced by the old services: it may already
                    // have been accepted by SOU.
                    return false;
                }
                PaymentEntity locked = payments.findByIdForUpdate(storno.getId()).orElseThrow();
                StornoRequest synthetic = new StornoRequest(
                        terminalId,
                        retailId == null ? 0L : retailId,
                        trxId,
                        locked.getShiftNumber() == null ? 0 : locked.getShiftNumber(),
                        null,
                        locked.getReceiptNumber() == null ? null : Integer.valueOf(locked.getReceiptNumber()),
                        new MoneyDto(locked.getAmount(), locked.getDiscount(),
                                locked.getCurrency(), locked.getCurrencyAbbr()),
                        locked.getQrPayClientId(),
                        null,
                        null,
                        null,
                        timeZoneId,
                        locked.getTerminalIp(),
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
                .map(this::isCompleted)
                .orElse(false);
    }

    private PaymentEntity createOrGet(StornoRequest request) {
        return tx.inTx(() -> {
            payments.acquireTransactionLock(store.lockKey(request.terminalId(), request.trxId()));
            String stornoTrx = TrxUtils.withSuffix(request.trxId(), "-S");
            Optional<PaymentEntity> existing = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                    request.terminalId(), stornoTrx, PaymentType.STORNO);
            if (existing.isPresent()) {
                return existing.get();
            }

            Optional<PaymentEntity> sale = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                    request.terminalId(), request.trxId(), PaymentType.SALE);
            PaymentEntity storno = new PaymentEntity();
            storno.setCashDeskUid(request.terminalId());
            storno.setTrnId(stornoTrx);
            storno.setPaymentDate(TimeZones.toUtc(
                    TimeZones.nowInZone(clock, request.timeZoneId()), request.timeZoneId()));
            storno.setPaymentType(PaymentType.STORNO);
            storno.setSuccess(false);
            storno.setShiftNumber(request.shiftNo());
            storno.setReceiptNumber(request.receiptNumber() == null ? null : String.valueOf(request.receiptNumber()));
            storno.setTerminalIp(request.terminalIp());
            storno.setState(sale.isPresent() ? PaymentState.CREATED : PaymentState.STORNO_PENDING);
            applySaleOrRequest(storno, sale.orElse(null), request);

            payments.save(storno);
            store.saveReceipts(storno.getId(), request.receipts(),
                    new MoneyDto(storno.getAmount(), storno.getDiscount(), storno.getCurrency(),
                            storno.getCurrencyAbbr()), request.trxId());
            return storno;
        });
    }

    private OperationResponse execute(Long id, StornoRequest request, LocalDateTime now) {
        return tx.inTx(() -> {
            PaymentEntity storno = payments.findByIdForUpdate(id).orElseThrow();
            List<ReceiptEntity> receipts = store.receipts(id);

            if (isFinal(storno)) {
                return responseFactory.stored(storno, receipts, now);
            }

            SouPaymentResponse response = doExecute(storno, request);
            if (response == null) {
                return responseFactory.error(ApplicationError.SALE_TRANSACTION_NOT_FOUND,
                        ApplicationError.SALE_TRANSACTION_NOT_FOUND.getMessage(), now);
            }
            return responseFactory.of(storno, receipts, response, now);
        });
    }

    /**
     * Runs with the storno row already locked in the current transaction. Returns the
     * SOU response, or {@code null} when the sale has not succeeded yet.
     */
    private SouPaymentResponse doExecute(PaymentEntity storno, StornoRequest request) {
        Optional<PaymentEntity> saleOpt = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                storno.getCashDeskUid(), TrxUtils.clearTrx(storno.getTrnId()), PaymentType.SALE);
        if (saleOpt.isEmpty() || !saleOpt.get().isSuccess()) {
            storno.setState(PaymentState.STORNO_PENDING);
            payments.save(storno);
            return null;
        }

        PaymentEntity sale = saleOpt.get();
        // Existing cash-desk behaviour: the storno amount always follows the sale.
        storno.setAmount(sale.getAmount());
        storno.setDiscount(sale.getDiscount());
        storno.setCurrency(sale.getCurrency());
        storno.setCurrencyAbbr(sale.getCurrencyAbbr());
        storno.setQrPayClientId(sale.getQrPayClientId());

        SouPaymentResponse response;
        try {
            response = souClient.storno(souFactory.stornoRequest(storno, request));
        } catch (FeignException ex) {
            throw new SouUnavailableException("SOU storno call failed for trx " + storno.getTrnId(), ex);
        }

        if (response.isSuccess()) {
            storno.setState(PaymentState.COMPLETED);
            storno.setSuccess(true);
            storno.setSouId(response.getSouTransaction() == null ? 0L : response.getSouTransaction());
            LocalDateTime souTime = response.operationTimeAsLocalDateTime();
            if (souTime != null) {
                storno.setPaymentDate(TimeZones.toUtc(souTime, request.timeZoneId()));
            }
            sale.setStorno(true);
            payments.save(sale);
        } else {
            storno.setState(PaymentState.DECLINED);
            storno.setSuccess(false);
            storno.setErrorText(response.getErrorMessage());
        }
        payments.save(storno);
        return response;
    }

    private void applySaleOrRequest(PaymentEntity storno, PaymentEntity sale, StornoRequest request) {
        if (sale != null) {
            storno.setAmount(sale.getAmount());
            storno.setDiscount(sale.getDiscount());
            storno.setCurrency(sale.getCurrency());
            storno.setCurrencyAbbr(sale.getCurrencyAbbr());
            storno.setQrPayClientId(sale.getQrPayClientId());
        } else {
            storno.setAmount(request.money().amount());
            storno.setDiscount(request.money().discount());
            storno.setCurrency(request.money().currency());
            storno.setCurrencyAbbr(request.money().currencyAbbr());
            storno.setQrPayClientId(request.qrPayClientId());
        }
    }

    private boolean isFinal(PaymentEntity storno) {
        // LEGACY rows belong to the old services and must never be retried: a second
        // storno could move money again. Only CREATED/STORNO_PENDING are retryable.
        return storno.getState().isSettled();
    }

    private boolean isCompleted(PaymentEntity storno) {
        return storno.getState() == PaymentState.COMPLETED
                || (storno.getState() == PaymentState.LEGACY && storno.isSuccess());
    }
}
