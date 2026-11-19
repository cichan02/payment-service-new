package com.besmart.qrpay.paymentservice.domain;

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
