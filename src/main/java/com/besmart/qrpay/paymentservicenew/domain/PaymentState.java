package com.besmart.qrpay.paymentservicenew.domain;

/**
 * Lifecycle of a payment row.
 *
 * <p>{@link #LEGACY} is written by older services that do not know about this column
 * (database default). Such rows must never be retried against SOU, because the retry
 * could charge the customer twice.</p>
 */
public enum PaymentState {
    LEGACY,
    CREATED,
    CONFIRMED,
    COMPLETED,
    DECLINED,
    CANCELLED,
    STORNO_PENDING;

    /**
     * A settled row must never be sent to SOU again: the remote side may have already
     * applied it and a retry could move money twice. {@code CREATED} is the only state
     * that explicitly allows a retry.
     */
    public boolean isSettled() {
        return this != CREATED && this != STORNO_PENDING;
    }
}
