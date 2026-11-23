package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.PaymentHistory;
import com.besmart.qrpay.paymentservicenew.core.domain.PaymentHistoryQuery;

public interface PaymentHistoryService {

    PaymentHistory history(PaymentHistoryQuery query);
}
