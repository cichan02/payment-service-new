package com.besmart.qrpay.paymentservicenew.infra.network.to.request;

import java.math.BigDecimal;

public record SouGoodRequest(
        String productName,
        BigDecimal amount,
        Short currency,
        BigDecimal discount,
        String description,
        BigDecimal price,
        String unitName,
        BigDecimal quantity,
        String barcode
) {
}
