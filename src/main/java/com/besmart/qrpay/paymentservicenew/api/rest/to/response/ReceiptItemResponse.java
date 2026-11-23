package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import com.besmart.qrpay.paymentservicenew.api.rest.to.common.Money;

import java.math.BigDecimal;

public record ReceiptItemResponse(
        Long id,
        Long paymentId,
        String name,
        BigDecimal price,
        BigDecimal quantity,
        String unitName,
        String barCode,
        Money money
) {
}
