package com.besmart.qrpay.paymentservicenew.core.domain;

import java.math.BigDecimal;

/**
 * Money fields always travelling together: amount, discount, currency code and
 * currency abbreviation. Domain code passes them around as one value so they cannot
 * drift apart.
 */
public record Money(BigDecimal amount, BigDecimal discount, Integer currency, String currencyAbbr) {

    public Money {
        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
    }
}
