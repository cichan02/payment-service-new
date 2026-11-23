package com.besmart.qrpay.paymentservicenew.core.domain.model;

import java.time.LocalDateTime;
import java.util.Collection;

public record PaymentHistory(LocalDateTime operationTime, Collection<Item> items) {
    public record Item(
        Payment payment,
        Collection<Receipt> receipts
    ) {}
}
