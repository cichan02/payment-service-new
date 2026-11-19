package com.besmart.qrpay.paymentservice.service;

import com.besmart.qrpay.paymentservice.api.dto.PaymentRequest;
import com.besmart.qrpay.paymentservice.domain.MoneyDto;
import com.besmart.qrpay.paymentservice.domain.OperationResponse;
import com.besmart.qrpay.paymentservice.domain.PaymentState;
import com.besmart.qrpay.paymentservice.domain.PaymentType;
import com.besmart.qrpay.paymentservice.entity.PaymentEntity;
import com.besmart.qrpay.paymentservice.entity.ReceiptEntity;
import com.besmart.qrpay.paymentservice.error.ApplicationError;
import com.besmart.qrpay.paymentservice.error.SouUnavailableException;
import com.besmart.qrpay.paymentservice.mapper.ResponseFactory;
import com.besmart.qrpay.paymentservice.repository.PaymentRepository;
import com.besmart.qrpay.paymentservice.sou.SouClient;
import com.besmart.qrpay.paymentservice.sou.SouRequestFactory;
import com.besmart.qrpay.paymentservice.sou.dto.SouPaymentResponse;
import com.besmart.qrpay.paymentservice.util.TimeZones;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TxRunner tx;
    private final PaymentRepository payments;
    private final PaymentStore store;
    private final SouClient souClient;
    private final SouRequestFactory souFactory;
    private final StornoService stornoService;
    private final Clock clock;

    /**
     * Payment operation. Two transactions: the intent row is committed first
     * (state CREATED), then SOU is called under a row lock. A crash between the two
     * leaves a durable CREATED row and the same request can be retried.
     */
    public OperationResponse pay(PaymentRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        PaymentEntity payment = createOrGet(request);
        List<ReceiptEntity> receipts = store.receipts(payment.getId());

        if (isFinal(payment.getState())) {
            return cancelledOrStored(request, payment, receipts, now);
        }

        Execution execution = tx.inTx(() -> execute(payment.getId(), request));
        List<ReceiptEntity> resultReceipts = store.receipts(execution.payment().getId());

        if (execution.souResponse() == null) {
            return cancelledOrStored(request, execution.payment(), resultReceipts, now);
        }
        if (execution.souResponse().isSuccess()
                && stornoService.executePendingAfterPayment(request.terminalId(), request.trxId(),
                request.retailId(), request.timeZoneId())) {
            log.info("Payment {} was cancelled by a delayed storno", request.trxId());
            return ResponseFactory.error(ApplicationError.PAYMENT_CANCELLED,
                    ApplicationError.PAYMENT_CANCELLED.getMessage(), now);
        }
        return ResponseFactory.of(execution.payment(), resultReceipts, execution.souResponse(), now);
    }

    private OperationResponse cancelledOrStored(PaymentRequest request, PaymentEntity payment,
                                                List<ReceiptEntity> receipts, LocalDateTime now) {
        // A duplicate request is also the recovery path for a delayed storno whose SOU
        // call failed earlier: the payment is already final, so nothing else would
        // retry it.
        if (payment.isSuccess()) {
            stornoService.executePendingAfterPayment(request.terminalId(), request.trxId(),
                    request.retailId(), request.timeZoneId());
        }
        if (stornoService.isCompleted(request.terminalId(), request.trxId())) {
            return ResponseFactory.error(ApplicationError.PAYMENT_CANCELLED,
                    ApplicationError.PAYMENT_CANCELLED.getMessage(), now);
        }
        return ResponseFactory.stored(payment, receipts, now);
    }

    private PaymentEntity createOrGet(PaymentRequest request) {
        return tx.inTx(() -> {
            payments.acquireTransactionLock(store.lockKey(request.terminalId(), request.trxId()));
            Optional<PaymentEntity> existing = payments.findByCashDeskUidAndTrnIdAndPaymentType(
                    request.terminalId(), request.trxId(), PaymentType.SALE);
            if (existing.isPresent()) {
                store.assertSameTransaction(existing.get(), request.money());
                return existing.get();
            }

            PaymentEntity payment = new PaymentEntity();
            payment.setCashDeskUid(request.terminalId());
            payment.setTrnId(request.trxId());
            payment.setPaymentDate(TimeZones.toUtc(
                    TimeZones.nowInZone(clock, request.timeZoneId()), request.timeZoneId()));
            payment.setAmount(request.money().amount());
            payment.setDiscount(request.money().discount());
            payment.setCurrency(request.money().currency());
            payment.setCurrencyAbbr(request.money().currencyAbbr());
            payment.setShiftNumber(request.shiftNo());
            payment.setReceiptNumber(String.valueOf(request.receiptNumber()));
            payment.setQrPayClientId(request.qrPayClientId());
            payment.setPayToolId(request.payToolId());
            payment.setSmartPaymentCardNumber(request.smartPaymentCardNumber());
            payment.setTerminalIp(request.terminalIp());
            payment.setPaymentType(PaymentType.SALE);
            payment.setSuccess(false);
            payment.setState(PaymentState.CREATED);

            payments.save(payment);
            store.saveReceipts(payment.getId(), request.receipts(), request.money(), request.trxId());
            return payment;
        });
    }

    private Execution execute(Long id, PaymentRequest request) {
        return tx.inTx(() -> {
            PaymentEntity payment = payments.findByIdForUpdate(id).orElseThrow();
            if (isFinal(payment.getState())) {
                return new Execution(payment, null);
            }
            List<ReceiptEntity> receipts = store.receipts(id);

            SouPaymentResponse response;
            try {
                response = souClient.payment(souFactory.paymentRequest(payment, request, receipts));
            } catch (FeignException ex) {
                throw new SouUnavailableException("SOU payment call failed for trx " + payment.getTrnId(), ex);
            }

            if (response.isSuccess()) {
                payment.setState(PaymentState.COMPLETED);
                payment.setSuccess(true);
                payment.setSouId(response.getSouTransaction() == null ? 0L : response.getSouTransaction());
                LocalDateTime souTime = response.operationTimeAsLocalDateTime();
                if (souTime != null) {
                    payment.setPaymentDate(TimeZones.toUtc(souTime, request.timeZoneId()));
                }
            } else {
                payment.setState(PaymentState.DECLINED);
                payment.setSuccess(false);
                payment.setErrorText(response.getErrorMessage());
            }
            payments.save(payment);
            return new Execution(payment, response);
        });
    }

    private boolean isFinal(PaymentState state) {
        return state.isSettled();
    }

    private record Execution(PaymentEntity payment, SouPaymentResponse souResponse) {
    }
}
