package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.ConfirmCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.OperationResult;

public interface ConfirmService {

    OperationResult confirm(ConfirmCommand command);
}
