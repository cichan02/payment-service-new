package com.besmart.qrpay.paymentservicenew.infra.network.to.response;

import com.besmart.qrpay.paymentservicenew.core.util.TimeZones;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record SouPaymentResponse(
        /** SOU sends the legacy {@code dd.MM.yyyy HH:mm:ss} pattern; kept as text. */
        String operationTime,
        int errorCode,
        String errorMessage,
        Long souTransaction,
        String slip,
        @JsonProperty("isEpay") Boolean isEpay
) {

    public boolean isSuccess() {
        return errorCode == 0;
    }

    public LocalDateTime operationTimeAsLocalDateTime() {
        return TimeZones.parseSouOperationTime(operationTime);
    }
}
