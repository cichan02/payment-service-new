package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.OperationResult;
import com.besmart.qrpay.paymentservicenew.core.domain.PaymentCommand;

public interface StornoService {

    OperationResult storno(PaymentCommand command);

    boolean executePendingAfterPayment(String terminalId, String trxId, Long retailId, String timeZoneId);

    boolean isCompleted(String terminalId, String trxId);
}
