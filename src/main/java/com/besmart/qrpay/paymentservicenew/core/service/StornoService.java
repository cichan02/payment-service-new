package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.api.rest.to.request.StornoRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;

public interface StornoService {

    OperationResponse storno(StornoRequest request);

    boolean executePendingAfterPayment(String terminalId, String trxId, Long retailId, String timeZoneId);

    boolean isCompleted(String terminalId, String trxId);
}
