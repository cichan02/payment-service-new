package com.besmart.qrpay.paymentservicenew.api.rest.to.response;


import java.time.LocalDateTime;
import java.util.List;

public record OperationResponse(
        int errorCode,
        String errorMessage,
        LocalDateTime operationTime,
        String trxId,
        Long operationId,
        String slip,
        List<String> receiptSlip,
        PaymentResponse payment,
        List<ReceiptItemResponse> receipts
) {
}
