package com.besmart.qrpay.paymentservicenew.core.domain;

import java.util.Collection;

public record PaymentHistoryEntry(Payment payment, Collection<Receipt> receipts) {
}
