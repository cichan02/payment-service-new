package com.besmart.qrpay.paymentservice.sou.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceConfirmResponse {

    private Integer errorCode;
    private String errorMessage;
    private String operationTime;

    public boolean isSuccess() {
        return errorCode != null && errorCode == 0;
    }
}
