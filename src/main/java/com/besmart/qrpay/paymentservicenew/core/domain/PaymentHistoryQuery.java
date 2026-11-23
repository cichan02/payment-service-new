package com.besmart.qrpay.paymentservicenew.core.domain;

public record PaymentHistoryQuery(
        Long qrPayClientId,
        Long dateFrom,
        Long dateTo,
        Long payToolId,
        String cardNumber
) {
}
