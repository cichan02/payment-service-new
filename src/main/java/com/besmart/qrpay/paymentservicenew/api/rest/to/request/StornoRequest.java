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
 * Cash-desk side cancellation of a transaction. The transaction is identified by
 * {@code terminalId + trxId}; the storno row is stored with the {@code -S} suffix.
 */
public record StornoRequest(
        @NotBlank String terminalId,
        @NotNull @Positive Long retailId,
        @NotBlank String trxId,
        @NotNull @PositiveOrZero Integer shiftNo,
        LocalDate shiftDate,
        Integer receiptNumber,
        @NotNull @Valid Money money,
        Long qrPayClientId,
        String operatorId,
        String operatorName,
        String deviceNo,
        String timeZoneId,
        String terminalIp,
        @Valid List<ReceiptItemRequest> receipts
) {
}
