package com.besmart.qrpay.paymentservice.domain;

/**
 * Values persisted in {@code public.payment.payment_type}.
 *
 * <p>The database historically uses {@code SALE} for a normal payment. That value is
 * kept for compatibility with cash-desk reports and the {@code payments_view}; the
 * public API calls the same operation "payment".</p>
 */
public enum PaymentType {
    SALE,
    STORNO,
    REFUND
}
