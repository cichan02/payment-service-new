package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.result.OperationResult;

public interface StornoService {

    OperationResult storno(PaymentCommand command);

    boolean executePendingAfterPayment(String terminalId, String trxId, Long retailId, String timeZoneId);

    boolean isCompleted(String terminalId, String trxId);
}
