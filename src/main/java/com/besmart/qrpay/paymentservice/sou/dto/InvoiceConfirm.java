package com.besmart.qrpay.paymentservice.sou.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceConfirm {

    private String appId;
    private Long invoiceId;
    private Long souId;

    @JsonProperty("identId")
    private Long paymentToolId;

    private Boolean success;
}
