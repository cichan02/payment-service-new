package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;nResult;

public interface RefundService {

    OperationResult refund(PaymentCommand command);
}
