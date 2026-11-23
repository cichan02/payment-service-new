package com.besmart.qrpay.paymentservicenew.api.rest.controller;

import com.besmart.qrpay.paymentservicenew.api.rest.mapper.PaymentRestMapper;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.PaymentRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.RefundRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.StornoRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;
import com.besmart.qrpay.paymentservicenew.core.service.PaymentService;
import com.besmart.qrpay.paymentservicenew.core.service.RefundService;
import com.besmart.qrpay.paymentservicenew.core.service.StornoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cash-desk-api facing operations. Decryption, QR parsing, OTP, loyalty, age
 * verification and terminal/store resolution happen in cash-desk-api before the call.
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final StornoService stornoService;
    private final RefundService refundService;
    private final PaymentRestMapper mapper;

    @PostMapping(path = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OperationResponse payment(@Valid @RequestBody PaymentRequest request) {
        return mapper.toResponse(paymentService.pay(mapper.toDomain(request)));
    }

    @PostMapping(path = "/storno", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OperationResponse storno(@Valid @RequestBody StornoRequest request) {
        return mapper.toResponse(stornoService.storno(mapper.toDomain(request)));
    }

    @PostMapping(path = "/refund", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OperationResponse refund(@Valid @RequestBody RefundRequest request) {
        return mapper.toResponse(refundService.refund(mapper.toDomain(request)));
    }
}
