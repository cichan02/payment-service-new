package com.besmart.qrpay.paymentservicenew.core.service.impl;

import com.besmart.qrpay.paymentservicenew.api.rest.mapper.ResponseFactory;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.RefundRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.ApplicationError;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.BusinessException;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.SouUnavailableException;
import com.besmart.qrpay.paymentservicenew.core.service.RefundService;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final TxRunner tx;
    private final PaymentRepository payments;
    private final PaymentStore store;
    private final SouClient souClient;
    private final SouRequestFactory souFactory;
    private final ResponseFactory responseFactory;
    private final Clock clock;

    /**
     * Customer side cancellation. The refund is a new transaction stored with the
     * {@code -R} suffix; {@code originalSouTransactionId} points at the payment being
     * refunded.
     */
    @Override
    public OperationResponse refund(RefundRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        PaymentEntity refund = createOrGet(request);

        if (isFinal(refund)) {
            return responseFactory.stored(refund, store.receipts(refund.getId()), now);
        }
        return execute(refund.getId(), request, now);
    }

    private PaymentEntity createOrGet(RefundRequest request) {
        return tx.inTx(() -> {
            payments.acquireTransactionLock(store.lockKey(request.terminalId(), request.trxId()));
            String refundTrx = TrxUtils.withSuffix(request.trxId(), "-R");
            Optional<PaymentEntity> existing = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                    request.terminalId(), refundTrx, PaymentType.REFUND);
            if (existing.isPresent()) {
                store.assertSameTransaction(existing.get(), request.money());
                return existing.get();
            }

            PaymentEntity original = payments.findFirstBySouIdOrderByIdAsc(request.originalSouTransactionId())
                    .orElseThrow(() -> new BusinessException(ApplicationError.ORIGINAL_PAYMENT_NOT_FOUND,
                            HttpStatus.NOT_FOUND, "No payment with souId " + request.originalSouTransactionId()));

            PaymentEntity refund = new PaymentEntity();
            refund.setCashDeskUid(request.terminalId());
            refund.setTrnId(refundTrx);
            refund.setPaymentDate(TimeZones.toUtc(
                    TimeZones.nowInZone(clock, request.timeZoneId()), request.timeZoneId()));
            refund.setPaymentType(PaymentType.REFUND);
            refund.setSuccess(false);
            refund.setState(PaymentState.CREATED);
            refund.setAmount(request.money().amount());
            refund.setDiscount(request.money().discount());
            refund.setCurrency(request.money().currency());
            refund.setCurrencyAbbr(request.money().currencyAbbr());
            refund.setShiftNumber(request.shiftNo());
            refund.setReceiptNumber(request.receiptNumber() == null ? null : String.valueOf(request.receiptNumber()));
            refund.setTerminalIp(request.terminalIp());
            // Existing behaviour: the refund inherits the client and pay tool from the
            // payment being refunded, so it is visible in the client's history.
            refund.setQrPayClientId(original.getQrPayClientId());
            refund.setPayToolId(original.getPayToolId());

            payments.save(refund);
            store.saveReceipts(refund.getId(), request.receipts(), request.money(), request.trxId());
            return refund;
        });
    }

    private OperationResponse execute(Long id, RefundRequest request, LocalDateTime now) {
        return tx.inTx(() -> {
            PaymentEntity refund = payments.findByIdForUpdate(id).orElseThrow();
            List<ReceiptEntity> receipts = store.receipts(id);
            if (isFinal(refund)) {
                return responseFactory.stored(refund, receipts, now);
            }

            SouPaymentResponse response;
            try {
                response = souClient.refund(souFactory.refundRequest(refund, request, receipts));
            } catch (FeignException ex) {
                throw new SouUnavailableException("SOU refund call failed for trx " + refund.getTrnId(), ex);
            }

            if (response.isSuccess()) {
                refund.setState(PaymentState.COMPLETED);
                refund.setSuccess(true);
                refund.setSouId(response.getSouTransaction() == null ? 0L : response.getSouTransaction());
                LocalDateTime souTime = response.operationTimeAsLocalDateTime();
                if (souTime != null) {
                    refund.setPaymentDate(TimeZones.toUtc(souTime, request.timeZoneId()));
                }
            } else {
                refund.setState(PaymentState.DECLINED);
                refund.setSuccess(false);
                refund.setErrorText(response.getErrorMessage());
            }
            payments.save(refund);
            return responseFactory.of(refund, receipts, response, now);
        });
    }

    private boolean isFinal(PaymentEntity refund) {
        return switch (refund.getState()) {
            case COMPLETED, DECLINED, CANCELLED, LEGACY -> true;
            default -> false;
        };
    }
}
