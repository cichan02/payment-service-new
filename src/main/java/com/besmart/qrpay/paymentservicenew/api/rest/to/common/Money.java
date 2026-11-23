package com.besmart.qrpay.paymentservicenew.api.rest.to.common;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Money fields always travelling together: amount, discount, currency code and
 * currency abbreviation. Used as a nested object in requests and responses so the
 * four values cannot drift apart.
 */
public record Money(
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @DecimalMin("0.00") BigDecimal discount,
        @NotNull Integer currency,
        String currencyAbbr
) {
    public Money {
        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
    }
}
