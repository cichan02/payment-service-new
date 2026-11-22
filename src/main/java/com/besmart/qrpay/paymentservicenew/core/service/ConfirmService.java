package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.api.rest.to.request.ConfirmRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;

public interface ConfirmService {

    OperationResponse confirm(ConfirmRequest request);
}
