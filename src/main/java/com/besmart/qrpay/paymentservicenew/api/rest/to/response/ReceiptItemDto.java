package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import com.besmart.qrpay.paymentservicenew.api.rest.to.common.MoneyDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceiptItemDto(
        Long id,
        Long paymentId,
        String name,
        BigDecimal price,
        BigDecimal quantity,
        String unitName,
        String barCode,
        MoneyDto money
) {
}
