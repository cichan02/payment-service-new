package com.besmart.qrpay.paymentservicenew.core.domain;

import java.time.LocalDateTime;
import java.util.Collection;

public record PaymentHistory(LocalDateTime operationTime, Collection<PaymentHistoryEntry> entries) {
}
