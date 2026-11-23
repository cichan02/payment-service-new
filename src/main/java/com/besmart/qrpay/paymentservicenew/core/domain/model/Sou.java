package com.besmart.qrpay.paymentservicenew.core.domain.model;

import java.time.LocalDateTime;

/**
 * What SOU answered, in domain terms. The network layer is the only place that knows
 * about {@code SouPaymentResponse}/{@code InvoiceConfirmResponse}.
 */
public record Sou(
        boolean success,
        int errorCode,
        String errorMessage,
        Long souTransactionId,
        String slip,
        LocalDateTime operationTime
) {
}
