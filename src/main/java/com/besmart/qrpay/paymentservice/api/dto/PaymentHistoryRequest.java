package com.besmart.qrpay.paymentservice.api.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentHistoryRequest(
        @NotNull Long qrPayClientId,
        @NotNull Long dateFrom,
        @NotNull Long dateTo,
        Long payToolId,
        String cardNumber
) {
}
