package com.besmart.qrpay.paymentservicenew.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Explicit transaction boundaries. Commands use two transactions on purpose:
 * the intent row is committed before the remote SOU call, so a crash cannot lose the
 * fact that a payment was attempted.
 */
@Component
@RequiredArgsConstructor
public class TxRunner {

    private final PlatformTransactionManager transactionManager;

    public <T> T inTx(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> action.get());
    }
}
