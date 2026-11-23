package com.besmart.qrpay.paymentservicenew.infra.network.client;

import com.besmart.qrpay.paymentservicenew.infra.network.to.request.InvoiceConfirmRequest;
import com.besmart.qrpay.paymentservicenew.infra.network.to.request.SouPaymentRequest;
import com.besmart.qrpay.paymentservicenew.infra.network.to.request.SouTerminalDataRequest;
import com.besmart.qrpay.paymentservicenew.infra.network.to.response.InvoiceConfirmResponse;
import com.besmart.qrpay.paymentservicenew.infra.network.to.response.SouPaymentResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "sou-service", url = "${sou.url:http://sou-service}", path = "/sou-service/api/v1")
public interface SouClient {

    @PostMapping(path = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    SouPaymentResponse payment(@RequestBody SouPaymentRequest payment);

    @PostMapping(path = "/storno", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    SouPaymentResponse storno(@RequestBody SouTerminalDataRequest terminalData);

    @PostMapping(path = "/refund", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    SouPaymentResponse refund(@RequestBody SouPaymentRequest payment);

    @PostMapping(path = "/invoicePayment", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    InvoiceConfirmResponse invoiceConfirm(@RequestBody InvoiceConfirmRequest confirm);
}
