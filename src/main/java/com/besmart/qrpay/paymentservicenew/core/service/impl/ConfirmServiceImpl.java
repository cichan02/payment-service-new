package com.besmart.qrpay.paymentservicenew.core.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.besmart.qrpay.paymentservicenew.core.domain.command.ConfirmCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.SouUnavailableException;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Sou;
import com.besmart.qrpay.paymentservicenew.core.domain.result.OperationResult;
import com.besmart.qrpay.paymentservicenew.core.service.ConfirmService;
import com.besmart.qrpay.paymentservicenew.core.service.TxRunner;
import com.besmart.qrpay.paymentservicenew.infra.network.client.SouClient;
import com.besmart.qrpay.paymentservicenew.infra.network.mapper.PaymentNetMapper;
import com.besmart.qrpay.paymentservicenew.infra.network.to.response.InvoiceConfirmResponse;
import com.besmart.qrpay.paymentservicenew.infra.persistence.PaymentStore;
import com.besmart.qrpay.paymentservicenew.infra.persistence.mapper.PaymentDbMapper;
import com.besmart.qrpay.paymentservicenew.infra.persistence.repository.PaymentRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mobile side invoice confirmation/cancellation. Signature and device session checks
 * stay in mobile-api. A successful confirm does not set {@code payment.success}, to
 * keep the current mobile-api behaviour.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmServiceImpl implements ConfirmService {

    private final TxRunner tx;
    private final PaymentRepository payments;
    private final PaymentStore store;
    private final PaymentDbMapper dbMapper;
    private final SouClient souClient;
    private final PaymentNetMapper netMapper;
    private final Clock clock;

    @Override
    public OperationResult confirm(ConfirmCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        return tx.inTx(() -> {
            String lockKey = command.trxId() == null || command.trxId().isBlank()
                    ? "confirm|" + command.appId() + "|" + command.invoiceId()
                    : "confirm|" + command.qrPayClientId() + "|" + command.trxId();
            payments.acquireTransactionLock(lockKey);

            Payment payment = findPayment(command);
            Collection<Receipt> receipts = payment == null
                    ? List.of()
                    : store.receipts(payment.getId());

            if (isDuplicate(payment, command)) {
                return success(payment, receipts, command.trxId(), now);
            }

            if (payment != null && command.success()) {
                if (command.payToolId() != null) {
                    payment.setPayToolId(command.payToolId());
                }
                if (hasText(command.smartPaymentCardNumber())) {
                    payment.setSmartPaymentCardNumber(command.smartPaymentCardNumber());
                }
                if (hasText(command.currencyAbbr())) {
                    payment.setCurrencyAbbr(command.currencyAbbr());
                }
                if (hasText(command.clientIp())) {
                    payment.setQrPayClientIp(command.clientIp());
                }
            }

            Sou sou;
            try {
                InvoiceConfirmResponse response = souClient.invoiceConfirm(netMapper.toNet(command));
                sou = netMapper.toDomain(response);
            } catch (FeignException ex) {
                throw new SouUnavailableException("SOU invoicePayment call failed for trx " + command.trxId(), ex);
            }

            if (payment != null) {
                if (sou.success()) {
                    // Only mark a payment that SOU has already accepted. A row still in
                    // CREATED belongs to an in-flight /payment call and must stay
                    // retryable.
                    if (payment.isSuccess() || payment.getState().isSettled()) {
                        if (command.success()) {
                            payment.confirm();
                        } else {
                            payment.cancel();
                        }
                    }
                } else {
                    // Same as mobile-api: only record the error, success is owned by
                    // the cash-desk payment flow.
                    payment.error(sou.errorMessage());
                }
                payments.save(dbMapper.toEntity(payment));
            }

            return new OperationResult(sou.errorCode(), sou.errorMessage() == null ? "" : sou.errorMessage(),
                    now, command.trxId(), null, null, payment, receipts);
        });
    }

    private Payment findPayment(ConfirmCommand command) {
        if (command.trxId() == null || command.trxId().isBlank()) {
            return null;
        }
        return payments.findFirstByQrPayClientIdAndTrnIdAndPaymentTypeOrderByIdDesc(
                        command.qrPayClientId(), command.trxId(), PaymentType.SALE)
                .map(dbMapper::toDomain)
                .orElse(null);
    }

    private boolean isDuplicate(Payment payment, ConfirmCommand command) {
        if (payment == null) {
            return false;
        }
        if (command.success()) {
            return payment.getState() == PaymentState.CONFIRMED;
        }
        return payment.getState() == PaymentState.CANCELLED;
    }

    private OperationResult success(Payment payment, Collection<Receipt> receipts,
                                    String trxId, LocalDateTime now) {
        return new OperationResult(0, "", now, trxId, null, null, payment, receipts);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
