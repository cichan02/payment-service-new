package com.besmart.qrpay.paymentservicenew.api.rest.to.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
