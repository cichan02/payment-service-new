package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.api.rest.to.request.RefundRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;

public interface RefundService {

    OperationResponse refund(RefundRequest request);
}
