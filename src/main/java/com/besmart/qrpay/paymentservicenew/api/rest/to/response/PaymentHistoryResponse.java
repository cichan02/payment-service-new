package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentHistoryResponse(
        int errorCode,
        String errorMessage,
        LocalDateTime operationTime,
        List<PaymentHistoryItem> payments
) {
}
