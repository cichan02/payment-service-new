package com.besmart.qrpay.paymentservicenew.core.domain.model;

import java.util.Collection;

public record PaymentHistoryItem(
	Payment payment,
	Collection<Receipt> receipts
) {}
