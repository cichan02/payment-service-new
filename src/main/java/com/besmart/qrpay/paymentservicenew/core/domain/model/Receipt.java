package com.besmart.qrpay.paymentservicenew.core.domain.model;

import java.math.BigDecimal;

/**
 * One line of a payment receipt. Mirrors {@code public.receipt} but belongs to the
 * domain, not to persistence.
 */
public record Receipt(
        Long id,
        Long paymentId,
        String name,
        BigDecimal price,
        Integer currency,
        BigDecimal quantity,
        String unitName,
        BigDecimal amount,
        BigDecimal discount,
        String barCode
) {
}
