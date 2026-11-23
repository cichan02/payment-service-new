package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.result.OperationResult;

public interface PaymentService {

    OperationResult pay(PaymentCommand command);
}
