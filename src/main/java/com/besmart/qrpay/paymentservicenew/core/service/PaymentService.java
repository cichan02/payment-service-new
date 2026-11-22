package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.api.rest.to.request.PaymentRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;

public interface PaymentService {

    OperationResponse pay(PaymentRequest request);
}
