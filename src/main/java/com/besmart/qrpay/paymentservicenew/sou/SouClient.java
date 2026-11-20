package com.besmart.qrpay.paymentservicenew.sou;

import com.besmart.qrpay.paymentservicenew.sou.dto.InvoiceConfirm;
import com.besmart.qrpay.paymentservicenew.sou.dto.InvoiceConfirmResponse;
import com.besmart.qrpay.paymentservicenew.sou.dto.SouPayment;
import com.besmart.qrpay.paymentservicenew.sou.dto.SouPaymentResponse;
import com.besmart.qrpay.paymentservicenew.sou.dto.SouTerminalData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "sou-service", url = "${sou.url:http://sou-service}", path = "/sou-service/api/v1")
public interface SouClient {

    @PostMapping(path = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    SouPaymentResponse payment(@RequestBody SouPayment payment);

    @PostMapping(path = "/storno", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    SouPaymentResponse storno(@RequestBody SouTerminalData terminalData);

    @PostMapping(path = "/refund", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    SouPaymentResponse refund(@RequestBody SouPayment payment);

    @PostMapping(path = "/invoicePayment", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    InvoiceConfirmResponse invoiceConfirm(@RequestBody InvoiceConfirm confirm);
}
