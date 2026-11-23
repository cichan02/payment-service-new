package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.command.ConfirmCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.result.OperationResult;

public interface ConfirmService {

    OperationResult confirm(ConfirmCommand command);
}
