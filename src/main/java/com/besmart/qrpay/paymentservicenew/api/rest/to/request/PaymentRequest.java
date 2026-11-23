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
 * Everything cash-desk-api has already resolved for a payment: terminal, client,
 * SOU id, pay tool, loyalty JSON, store time zone. Decryption, OTP, loyalty and
 * age checks stay in cash-desk-api.
 *
 * <p>{@code operationDateTime} is intentionally absent: this service generates it.</p>
 */
public record PaymentRequest(
        @NotBlank String terminalId,
        @NotNull @Positive Long retailId,
        @NotBlank String trxId,
        @NotNull @PositiveOrZero Integer shiftNo,
        LocalDate shiftDate,
        @NotNull Integer receiptNumber,
        @NotNull @Valid Money money,
        Long qrPayClientId,
        Long souId,
        Long payToolId,
        String smartPaymentCardNumber,
        String operatorId,
        String operatorName,
        String deviceNo,
        String timeZoneId,
        String terminalIp,
        String mobileQueue,
        String qrVersion,
        String loyaltyJson,
        @Valid List<ReceiptItemRequest> receipts
) {
}
