package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import com.besmart.qrpay.paymentservicenew.api.rest.to.common.Money;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String cashDeskUid,
        String trnId,
        LocalDateTime paymentDate,
        Money money,
        boolean success,
        Long souId,
        String paymentType,
        Integer shiftNumber,
        String receiptNumber,
        Long qrPayClientId,
        boolean storno,
        String state,
        String terminalIp,
        String errorText,
        Long payToolId,
        String smartPaymentCardNumber
) {
}
