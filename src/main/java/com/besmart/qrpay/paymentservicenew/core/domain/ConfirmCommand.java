package com.besmart.qrpay.paymentservicenew.core.domain;

/**
 * Mobile side confirmation or cancellation of an invoice.
 *
 * <p>{@code success = true} confirms the payment, {@code success = false} cancels it.
 * As in the current mobile-api code, a successful confirm does not set
 * {@code payment.success}.</p>
 */
public record ConfirmCommand(
        String appId,
        Long invoiceId,
        Long souId,
        Long qrPayClientId,
        String trxId,
        Long payToolId,
        String smartPaymentCardNumber,
        String currencyAbbr,
        String clientIp,
        Boolean success
) {
}
