package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import java.time.LocalDateTime;

public record ErrorResponse(
        int errorCode,
        String errorMessage,
        LocalDateTime operationTime
) {
}
