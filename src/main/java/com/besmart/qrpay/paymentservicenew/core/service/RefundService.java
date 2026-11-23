package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.OperationResult;
import com.besmart.qrpay.paymentservicenew.core.domain.PaymentCommand;

public interface RefundService {

    OperationResult refund(PaymentCommand command);
}
