package com.besmart.qrpay.paymentservicenew.core.service;

import com.besmart.qrpay.paymentservicenew.core.domain.model.PaymentHistory;
import com.besmart.qrpay.paymentservicenew.core.domain.query.PaymentHistoryQuery;

public interface PaymentHistoryService {

    PaymentHistory history(PaymentHistoryQuery query);
}
