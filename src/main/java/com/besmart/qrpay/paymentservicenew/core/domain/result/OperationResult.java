package com.besmart.qrpay.paymentservicenew.core.domain.result;

import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.ApplicationError;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Sou;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Domain result of a money-moving operation. The REST layer turns it into an
 * {@code OperationResponse}; the core never sees a response TO.
 */
public record OperationResult(
        int errorCode,
        String errorMessage,
        LocalDateTime operationTime,
        String trxId,
        Long operationId,
        String slip,
        Payment payment,
        Collection<Receipt> receipts
) {

    public static OperationResult error(ApplicationError error, LocalDateTime operationTime) {
        return new OperationResult(error.getErrorCode(), error.getMessage(), operationTime,
                null, null, null, null, null);
    }

    /**
     * Response for a duplicate request whose transaction is already final. The original
     * SOU response is not stored, so the error code is derived from the persisted state.
     */
    public static OperationResult stored(Payment payment, Collection<Receipt> receipts,
                                         LocalDateTime operationTime) {
        ApplicationError error = ApplicationError.SUCCESS;
        String message = "";

        if (payment.getState() == PaymentState.CANCELLED) {
            error = ApplicationError.PAYMENT_CANCELLED;
            message = error.getMessage();
        } else if (!payment.isSuccess() || payment.getState() == PaymentState.DECLINED
                || payment.getState() == PaymentState.LEGACY) {
            error = ApplicationError.PAYMENT_DECLINED;
            message = payment.getErrorText() == null ? error.getMessage() : payment.getErrorText();
        }

        return new OperationResult(error.getErrorCode(), message, operationTime,
                payment.getTrnId(), payment.getSouId(), null, payment, receipts);
    }

    public static OperationResult of(Payment payment, Collection<Receipt> receipts,
                                     Sou sou, LocalDateTime operationTime) {
        return new OperationResult(
                sou.errorCode(),
                sou.errorMessage() == null ? "" : sou.errorMessage(),
                operationTime,
                payment.getTrnId(),
                sou.souTransactionId() == null ? payment.getSouId() : sou.souTransactionId(),
                sou.slip(),
                payment,
                receipts
        );
    }
}
