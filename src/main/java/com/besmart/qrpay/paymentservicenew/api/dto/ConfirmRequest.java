package com.besmart.qrpay.paymentservicenew.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Mobile side confirmation or cancellation of an invoice. Verification of the app
 * signature and the device session stays in mobile-api.
 *
 * <p>{@code success = true} confirms the payment, {@code success = false} cancels it.
 * As in the current mobile-api code, a successful confirm does not set
 * {@code payment.success}.</p>
 */
public record ConfirmRequest(
        @NotBlank String appId,
        @NotNull Long invoiceId,
        @NotNull Long souId,
        @NotNull Long qrPayClientId,
        /**
         * Cash-desk transaction number. Optional because mobile-api's invoiceCancel
         * request does not carry one; when present it is used to update the payment row.
         */
        String trxId,
        Long payToolId,
        String smartPaymentCardNumber,
        String currencyAbbr,
        String clientIp,
        @NotNull Boolean success
) {
}
