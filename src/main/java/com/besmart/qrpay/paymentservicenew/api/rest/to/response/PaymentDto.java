package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import com.besmart.qrpay.paymentservicenew.api.rest.to.common.MoneyDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentDto(
        Long id,
        String cashDeskUid,
        String trnId,
        LocalDateTime paymentDate,
        MoneyDto money,
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
