package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.api.rest.to.request.PaymentHistoryRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.PaymentHistoryResponse;

public interface PaymentHistoryService {

    PaymentHistoryResponse history(PaymentHistoryRequest request);
}
