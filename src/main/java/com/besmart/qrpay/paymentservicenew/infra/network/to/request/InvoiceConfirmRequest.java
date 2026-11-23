package com.besmart.qrpay.paymentservicenew.infra.network.to.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InvoiceConfirmRequest(
        String appId,
        Long invoiceId,
        Long souId,
        @JsonProperty("identId") Long paymentToolId,
        Boolean success
) {
}
