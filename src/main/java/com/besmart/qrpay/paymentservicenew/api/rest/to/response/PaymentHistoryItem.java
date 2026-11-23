package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentHistoryItem(
        PaymentResponse payment,
        List<ReceiptItemResponse> receipts
) {
}
