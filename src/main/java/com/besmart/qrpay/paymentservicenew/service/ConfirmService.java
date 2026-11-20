package com.besmart.qrpay.paymentservicenew.service;

import com.besmart.qrpay.paymentservicenew.api.dto.ConfirmRequest;
import com.besmart.qrpay.paymentservicenew.domain.OperationResponse;
import com.besmart.qrpay.paymentservicenew.domain.PaymentState;
import com.besmart.qrpay.paymentservicenew.domain.PaymentType;
import com.besmart.qrpay.paymentservicenew.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.entity.ReceiptEntity;
import com.besmart.qrpay.paymentservicenew.error.SouUnavailableException;
import com.besmart.qrpay.paymentservicenew.mapper.PaymentMapper;
import com.besmart.qrpay.paymentservicenew.repository.PaymentRepository;
import com.besmart.qrpay.paymentservicenew.sou.SouClient;
import com.besmart.qrpay.paymentservicenew.sou.SouRequestFactory;
import com.besmart.qrpay.paymentservicenew.sou.dto.InvoiceConfirmResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mobile side invoice confirmation/cancellation. Signature and device session checks
 * stay in mobile-api. A successful confirm does not set {@code payment.success}, to
 * keep the current mobile-api behaviour.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmService {

    private final TxRunner tx;
    private final PaymentRepository payments;
    private final PaymentStore store;
    private final SouClient souClient;
    private final SouRequestFactory souFactory;
    private final Clock clock;

    public OperationResponse confirm(ConfirmRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        return tx.inTx(() -> {
            String lockKey = request.trxId() == null || request.trxId().isBlank()
                    ? "confirm|" + request.appId() + "|" + request.invoiceId()
                    : "confirm|" + request.qrPayClientId() + "|" + request.trxId();
            payments.acquireTransactionLock(lockKey);

            PaymentEntity payment = findPayment(request);
            List<ReceiptEntity> receipts = payment == null
                    ? List.of()
                    : store.receipts(payment.getId());

            if (isDuplicate(payment, request)) {
                return success(payment, receipts, request.trxId(), now);
            }

            if (payment != null && request.success()) {
                if (request.payToolId() != null) {
                    payment.setPayToolId(request.payToolId());
                }
                if (hasText(request.smartPaymentCardNumber())) {
                    payment.setSmartPaymentCardNumber(request.smartPaymentCardNumber());
                }
                if (hasText(request.currencyAbbr())) {
                    payment.setCurrencyAbbr(request.currencyAbbr());
                }
                if (hasText(request.clientIp())) {
                    payment.setQrPayClientIp(request.clientIp());
                }
            }

            InvoiceConfirmResponse response;
            try {
                response = souClient.invoiceConfirm(souFactory.confirmRequest(request));
            } catch (FeignException ex) {
                throw new SouUnavailableException("SOU invoicePayment call failed for trx " + request.trxId(), ex);
            }

            if (payment != null) {
                if (response.isSuccess()) {
                    // Only mark a payment that SOU has already accepted. A row still in
                    // CREATED belongs to an in-flight /payment call and must stay
                    // retryable.
                    if (payment.isSuccess() || payment.getState().isSettled()) {
                        payment.setState(request.success()
                                ? PaymentState.CONFIRMED
                                : PaymentState.CANCELLED);
                    }
                } else {
                    // Same as mobile-api: only record the error, success is owned by
                    // the cash-desk payment flow.
                    payment.setErrorText(response.getErrorMessage());
                }
                payments.save(payment);
            }

            return new OperationResponse(
                    response.getErrorCode() == null ? 0 : response.getErrorCode(),
                    response.getErrorMessage() == null ? "" : response.getErrorMessage(),
                    now,
                    request.trxId(),
                    null,
                    null,
                    null,
                    payment == null ? null : PaymentMapper.toDto(payment),
                    PaymentMapper.toReceiptDtos(receipts)
            );
        });
    }

    private PaymentEntity findPayment(ConfirmRequest request) {
        if (request.trxId() == null || request.trxId().isBlank()) {
            return null;
        }
        return payments.findFirstByQrPayClientIdAndTrnIdAndPaymentTypeOrderByIdDesc(
                        request.qrPayClientId(), request.trxId(), PaymentType.SALE)
                .orElse(null);
    }

    private boolean isDuplicate(PaymentEntity payment, ConfirmRequest request) {
        if (payment == null) {
            return false;
        }
        if (request.success()) {
            return payment.getState() == PaymentState.CONFIRMED;
        }
        return payment.getState() == PaymentState.CANCELLED;
    }

    private OperationResponse success(PaymentEntity payment, List<ReceiptEntity> receipts,
                                      String trxId, LocalDateTime now) {
        return new OperationResponse(0, "", now, trxId, null, null, null,
                PaymentMapper.toDto(payment), PaymentMapper.toReceiptDtos(receipts));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
