package com.besmart.qrpay.paymentservicenew.infra.network.to;

import com.besmart.qrpay.paymentservicenew.core.util.TimeZones;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SouPaymentResponse {

    /** SOU sends the legacy {@code dd.MM.yyyy HH:mm:ss} pattern; kept as text. */
    private String operationTime;
    private int errorCode;
    private String errorMessage;
    private Long souTransaction;
    private String slip;
    private Boolean isEpay;

    public boolean isSuccess() {
        return errorCode == 0;
    }

    public LocalDateTime operationTimeAsLocalDateTime() {
        return TimeZones.parseSouOperationTime(operationTime);
    }
}
