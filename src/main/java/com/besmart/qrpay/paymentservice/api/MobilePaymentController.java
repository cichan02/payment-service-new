package com.besmart.qrpay.paymentservice.api;

import com.besmart.qrpay.paymentservice.api.dto.ConfirmRequest;
import com.besmart.qrpay.paymentservice.api.dto.PaymentHistoryRequest;
import com.besmart.qrpay.paymentservice.domain.OperationResponse;
import com.besmart.qrpay.paymentservice.domain.PaymentHistoryResponse;
import com.besmart.qrpay.paymentservice.service.ConfirmService;
import com.besmart.qrpay.paymentservice.service.PaymentHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobile-api facing operations. App signature, device session and receipt rendering
 * stay in mobile-api.
 */
@RestController
@RequiredArgsConstructor
public class MobilePaymentController {

    private final ConfirmService confirmService;
    private final PaymentHistoryService paymentHistoryService;

    @PostMapping(path = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OperationResponse> confirm(@Valid @RequestBody ConfirmRequest request) {
        OperationResponse response = confirmService.confirm(request);
        return ResponseEntity
                .status(response.errorCode() == 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PostMapping(path = "/payment-history", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public PaymentHistoryResponse history(@Valid @RequestBody PaymentHistoryRequest request) {
        return paymentHistoryService.history(request);
    }
}
