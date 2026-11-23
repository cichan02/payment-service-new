package com.besmart.qrpay.paymentservicenew.api.rest.to.request;

import com.besmart.qrpay.paymentservicenew.api.rest.to.common.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;

/**
 * Customer side cancellation.
 *
 * <p>{@code originalSouTransactionId} is the SOU transaction number of the payment
 * being refunded (the old API called it {@code Refund.id}). The refund itself is a
 * new transaction and is stored with the {@code -R} suffix.</p>
 */
public record RefundRequest(
        @NotBlank String terminalId,
        @NotNull @Positive Long retailId,
        @NotBlank String trxId,
        @NotNull @PositiveOrZero Integer shiftNo,
        LocalDate shiftDate,
        Integer receiptNumber,
        @NotNull @Valid Money money,
        @NotNull Long originalSouTransactionId,
        Long qrPayClientId,
        Long payToolId,
        String operatorId,
        String operatorName,
        String deviceNo,
        String timeZoneId,
        String terminalIp,
        @Valid List<ReceiptItemRequest> receipts
) {
}
