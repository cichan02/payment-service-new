package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;nResult;

public interface PaymentService {

    OperationResult pay(PaymentCommand command);
}
