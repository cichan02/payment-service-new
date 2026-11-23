package com.besmart.qrpay.paymentservicenew.infra.network.mapper;

import com.besmart.qrpay.paymentservicenew.core.domain.command.ConfirmCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Sou;
import com.besmart.qrpay.paymentservicenew.core.util.TimeZones;
import com.besmart.qrpay.paymentservicenew.core.util.TrxUtils;
import com.besmart.qrpay.paymentservicenew.infra.network.to.InvoiceConfirm;
import com.besmart.qrpay.paymentservicenew.infra.network.to.InvoiceConfirmResponse;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouAGoods;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouGood;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouPayment;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouPaymentResponse;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouTerminalData;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Maps domain operations to SOU payloads and SOU answers back to the domain.
 *
 * <p>Payloads are built from the persisted row, never from "now". On a retry this
 * reproduces the original terminal date even if the retry happens minutes later.</p>
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public abstract class PaymentNetMapper {

    private static final short VALIDITY_SECONDS = 30;
    private static final int DEFAULT_CURRENCY = 933;

    public SouPayment toNet(Payment payment, PaymentCommand command, Collection<Receipt> receipts,
                            PaymentType paymentType) {
        SouPayment souPayment = new SouPayment();
        souPayment.setSouTerminalData(toNet(payment, command));
        souPayment.setAmount(payment.getAmount());
        souPayment.setProducts(goods(receipts, paymentType, payment.getTrnId(), payment));
        souPayment.setCurrency(currency(payment));
        souPayment.setValiditySecond(VALIDITY_SECONDS);
        souPayment.setCashDeskApiTrxId(payment.getId());

        if (paymentType == PaymentType.REFUND) {
            souPayment.setSouId(null);
            souPayment.setSouTransaction(command.originalSouTransactionId());
            souPayment.setMobileQueue(null);
            souPayment.setLoyaltyJson(null);
        } else {
            souPayment.setDiscount(zeroIfNull(payment.getDiscount()));
            souPayment.setSouId(command.souId());
            souPayment.setMobileQueue(command.mobileQueue());
            souPayment.setSmartPaymentCardNumber(command.smartPaymentCardNumber());
            souPayment.setLoyaltyJson(isBlank(command.loyaltyJson()) ? "[]" : command.loyaltyJson());
            souPayment.setPayToolId(command.payToolId());
            souPayment.setQrVersion(command.qrVersion());
        }

        return souPayment;
    }

    public SouTerminalData toNet(Payment payment, PaymentCommand command) {
        LocalDateTime operationLocal = TimeZones.fromUtc(payment.getPaymentDate(), command.timeZoneId());
        LocalDate effectiveShiftDate = command.shiftDate() != null
                ? command.shiftDate()
                : operationLocal.toLocalDate();

        SouTerminalData terminalData = new SouTerminalData();
        terminalData.setTerminalId(payment.getCashDeskUid());
        terminalData.setTrxId(TrxUtils.clearTrx(payment.getTrnId()));
        terminalData.setTerminalDate(TimeZones.format(operationLocal));
        terminalData.setShiftNum(payment.getShiftNumber());
        terminalData.setShiftDate(TimeZones.format(effectiveShiftDate.atStartOfDay()));
        terminalData.setCashierId(command.operatorId());
        terminalData.setCashierFIO(null);
        terminalData.setDeviceNo(command.deviceNo());
        return terminalData;
    }

    public InvoiceConfirm toNet(ConfirmCommand command) {
        InvoiceConfirm confirm = new InvoiceConfirm();
        confirm.setAppId(command.appId());
        confirm.setInvoiceId(command.invoiceId());
        confirm.setSouId(command.souId());
        confirm.setPaymentToolId(command.payToolId());
        confirm.setSuccess(command.success());
        return confirm;
    }

    public Sou toDomain(SouPaymentResponse response) {
        return new Sou(
                response.isSuccess(),
                response.getErrorCode(),
                response.getErrorMessage(),
                response.getSouTransaction(),
                response.getSlip(),
                response.operationTimeAsLocalDateTime()
        );
    }

    public Sou toDomain(InvoiceConfirmResponse response) {
        return new Sou(
                response.isSuccess(),
                response.getErrorCode() == null ? 0 : response.getErrorCode(),
                response.getErrorMessage(),
                null,
                null,
                TimeZones.parseSouOperationTime(response.getOperationTime())
        );
    }

    private SouAGoods goods(Collection<Receipt> receipts, PaymentType type, String trnId, Payment payment) {
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

    private SouGood good(Receipt receipt) {
        SouGood good = new SouGood();
        good.setProductName(receipt.name());
        good.setBarcode(Optional.ofNullable(receipt.barCode()).orElse(""));
        good.setQuantity(Optional.ofNullable(receipt.quantity()).orElse(BigDecimal.ONE));
        good.setAmount(receipt.amount());
        good.setDiscount(zeroIfNull(receipt.discount()));
        return good;
    }

    private SouGood defaultGood(PaymentType type, String trnId, Payment payment) {
        SouGood good = new SouGood();
        good.setProductName((type == PaymentType.REFUND ? "Возврат N " : "Оплата N ") + TrxUtils.clearTrx(trnId));
        good.setBarcode("");
        good.setQuantity(BigDecimal.ONE);
        good.setAmount(payment.getAmount());
        good.setDiscount(zeroIfNull(payment.getDiscount()));
        return good;
    }

    private Short currency(Payment payment) {
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
