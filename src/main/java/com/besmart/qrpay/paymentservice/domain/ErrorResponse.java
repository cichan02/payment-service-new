package com.besmart.qrpay.paymentservice.domain;

import java.time.LocalDateTime;

public record ErrorResponse(
        int errorCode,
        String errorMessage,
        LocalDateTime operationTime
) {
}
