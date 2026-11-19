package com.besmart.qrpay.paymentservice.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentHistoryItem(
        PaymentDto payment,
        java.util.List<ReceiptItemDto> receipts
) {
}
