package com.besmart.qrpay.paymentservicenew.api;

import com.besmart.qrpay.paymentservicenew.api.dto.PaymentRequest;
import com.besmart.qrpay.paymentservicenew.api.dto.RefundRequest;
import com.besmart.qrpay.paymentservicenew.api.dto.StornoRequest;
import com.besmart.qrpay.paymentservicenew.domain.OperationResponse;
import com.besmart.qrpay.paymentservicenew.service.PaymentService;
import com.besmart.qrpay.paymentservicenew.service.RefundService;
import com.besmart.qrpay.paymentservicenew.service.StornoService;
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

    @PostMapping(path = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OperationResponse payment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(request);
    }

    @PostMapping(path = "/storno", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OperationResponse storno(@Valid @RequestBody StornoRequest request) {
        return stornoService.storno(request);
    }

    @PostMapping(path = "/refund", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public OperationResponse refund(@Valid @RequestBody RefundRequest request) {
        return refundService.refund(request);
    }
}
