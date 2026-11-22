package com.besmart.qrpay.paymentservicenew.infra.network.mapper;

import com.besmart.qrpay.paymentservicenew.api.rest.to.request.ConfirmRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.PaymentRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.RefundRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.StornoRequest;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.ReceiptEntity;
import com.besmart.qrpay.paymentservicenew.infra.network.to.InvoiceConfirm;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouAGoods;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouGood;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouPayment;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouTerminalData;
import com.besmart.qrpay.paymentservicenew.core.util.TimeZones;
import com.besmart.qrpay.paymentservicenew.core.util.TrxUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds SOU payloads from the persisted row, never from "now". On a retry this
 * reproduces the original terminal date even if the retry happens minutes later.
 */
@Component
public class SouRequestFactory {

    private static final short VALIDITY_SECONDS = 30;
    private static final int DEFAULT_CURRENCY = 933;

    public SouPayment paymentRequest(PaymentEntity payment, PaymentRequest request,
                                     List<ReceiptEntity> receipts) {
        SouPayment souPayment = new SouPayment();
        souPayment.setSouTerminalData(terminalData(payment, request.timeZoneId(), request.shiftDate(),
                request.operatorId(), request.deviceNo()));
        souPayment.setAmount(payment.getAmount());
        souPayment.setDiscount(zeroIfNull(payment.getDiscount()));
        souPayment.setProducts(goods(receipts, PaymentType.SALE, payment.getTrnId(), payment));
        souPayment.setSouId(request.souId());
        souPayment.setCurrency(currency(payment));
        souPayment.setValiditySecond(VALIDITY_SECONDS);
        souPayment.setMobileQueue(request.mobileQueue());
        souPayment.setSmartPaymentCardNumber(request.smartPaymentCardNumber());
        souPayment.setLoyaltyJson(isBlank(request.loyaltyJson()) ? "[]" : request.loyaltyJson());
        souPayment.setPayToolId(request.payToolId());
        souPayment.setCashDeskApiTrxId(payment.getId());
        souPayment.setQrVersion(request.qrVersion());
        return souPayment;
    }

    public SouTerminalData stornoRequest(PaymentEntity storno, StornoRequest request) {
        return terminalData(storno, request.timeZoneId(), request.shiftDate(),
                request.operatorId(), request.deviceNo());
    }

    public SouPayment refundRequest(PaymentEntity refund, RefundRequest request,
                                    List<ReceiptEntity> receipts) {
        SouPayment souPayment = new SouPayment();
        souPayment.setSouTerminalData(terminalData(refund, request.timeZoneId(), request.shiftDate(),
                request.operatorId(), request.deviceNo()));
        souPayment.setAmount(refund.getAmount());
        souPayment.setProducts(goods(receipts, PaymentType.REFUND, refund.getTrnId(), refund));
        souPayment.setSouId(null);
        souPayment.setCurrency(currency(refund));
        souPayment.setValiditySecond(VALIDITY_SECONDS);
        souPayment.setMobileQueue(null);
        souPayment.setSouTransaction(request.originalSouTransactionId());
        souPayment.setCashDeskApiTrxId(refund.getId());
        souPayment.setLoyaltyJson(null);
        return souPayment;
    }

    public InvoiceConfirm confirmRequest(ConfirmRequest request) {
        InvoiceConfirm confirm = new InvoiceConfirm();
        confirm.setAppId(request.appId());
        confirm.setInvoiceId(request.invoiceId());
        confirm.setSouId(request.souId());
        confirm.setPaymentToolId(request.payToolId());
        confirm.setSuccess(request.success());
        return confirm;
    }

    private SouTerminalData terminalData(PaymentEntity payment, String timeZoneId, LocalDate shiftDate,
                                         String cashierId, String deviceNo) {
        String zone = timeZoneId;
        LocalDateTime operationLocal = TimeZones.fromUtc(payment.getPaymentDate(), zone);
        LocalDate effectiveShiftDate = shiftDate != null ? shiftDate : operationLocal.toLocalDate();

        SouTerminalData terminalData = new SouTerminalData();
        terminalData.setTerminalId(payment.getCashDeskUid());
        terminalData.setTrxId(TrxUtils.clearTrx(payment.getTrnId()));
        terminalData.setTerminalDate(TimeZones.format(operationLocal));
        terminalData.setShiftNum(payment.getShiftNumber());
        terminalData.setShiftDate(TimeZones.format(effectiveShiftDate.atStartOfDay()));
        terminalData.setCashierId(cashierId);
        terminalData.setCashierFIO(null);
        terminalData.setDeviceNo(deviceNo);
        return terminalData;
    }

    private SouAGoods goods(List<ReceiptEntity> receipts, PaymentType type, String trnId, PaymentEntity payment) {
        List<SouGood> goods = new ArrayList<>();
        if (receipts != null && !receipts.isEmpty()) {
            receipts.forEach(receipt -> goods.add(good(receipt)));
        } else {
            goods.add(defaultGood(type, trnId, payment));
        }
        SouAGoods souAGoods = new SouAGoods();
        souAGoods.setGoods(goods);
        return souAGoods;
    }

    private SouGood good(ReceiptEntity receipt) {
        SouGood good = new SouGood();
        good.setProductName(receipt.getName());
        good.setBarcode(Optional.ofNullable(receipt.getBarCode()).orElse(""));
        good.setQuantity(Optional.ofNullable(receipt.getQuantity()).orElse(BigDecimal.ONE));
        good.setAmount(receipt.getAmount());
        good.setDiscount(zeroIfNull(receipt.getDiscount()));
        return good;
    }

    private SouGood defaultGood(PaymentType type, String trnId, PaymentEntity payment) {
        SouGood good = new SouGood();
        good.setProductName((type == PaymentType.REFUND ? "Возврат N " : "Оплата N ") + TrxUtils.clearTrx(trnId));
        good.setBarcode("");
        good.setQuantity(BigDecimal.ONE);
        good.setAmount(payment.getAmount());
        good.setDiscount(zeroIfNull(payment.getDiscount()));
        return good;
    }

    private Short currency(PaymentEntity payment) {
        Integer currency = payment.getCurrency();
        return (short) (currency == null ? DEFAULT_CURRENCY : currency);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
