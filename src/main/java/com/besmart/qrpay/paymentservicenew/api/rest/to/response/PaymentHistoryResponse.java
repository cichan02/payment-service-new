package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import java.time.LocalDateTime;
import java.util.List;

public record PaymentHistoryResponse(
        int errorCode,
        String errorMessage,
        LocalDateTime operationTime,
        List<PaymentHistoryItem> payments
) {

    /**
     * One payment row of the history together with its receipt lines.
     */
    public record PaymentHistoryItem(
            PaymentResponse payment,
            List<ReceiptItemResponse> receipts
    ) {
    }
}
