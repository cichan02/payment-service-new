package com.besmart.qrpay.paymentservicenew.infra.network.to.response;

public record InvoiceConfirmResponse(
        Integer errorCode,
        String errorMessage,
        String operationTime
) {

    public boolean isSuccess() {
        return errorCode != null && errorCode == 0;
    }
}
