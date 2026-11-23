package com.besmart.qrpay.paymentservicenew.infra.network.mapper;

import com.besmart.qrpay.paymentservicenew.core.domain.command.ConfirmCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Sou;
import com.besmart.qrpay.paymentservicenew.core.util.TimeZones;
import com.besmart.qrpay.paymentservicenew.core.util.TrxUtils;
import com.besmart.qrpay.paymentservicenew.infra.network.to.request.InvoiceConfirmRequest;
import com.besmart.qrpay.paymentservicenew.infra.network.to.request.SouGoodRequest;
import com.besmart.qrpay.paymentservicenew.infra.network.to.request.SouPaymentRequest;
import com.besmart.qrpay.paymentservicenew.infra.network.to.request.SouTerminalDataRequest;
import com.besmart.qrpay.paymentservicenew.infra.network.to.response.InvoiceConfirmResponse;
import com.besmart.qrpay.paymentservicenew.infra.network.to.response.SouPaymentResponse;
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

    public SouPaymentRequest toNet(Payment payment, PaymentCommand command, Collection<Receipt> receipts,
                                   PaymentType paymentType) {
        boolean isRefund = paymentType == PaymentType.REFUND;
        return new SouPaymentRequest(
                toNet(payment, command),
                payment.getAmount(),
                isRefund ? null : zeroIfNull(payment.getDiscount()),
                goods(receipts, paymentType, payment.getTrnId(), payment),
                currency(payment),
                isRefund ? null : command.souId(),
                VALIDITY_SECONDS,
                isRefund ? null : command.mobileQueue(),
                isRefund ? null : command.smartPaymentCardNumber(),
                isRefund ? null : (isBlank(command.loyaltyJson()) ? "[]" : command.loyaltyJson()),
                isRefund ? command.originalSouTransactionId() : null,
                isRefund ? null : command.payToolId(),
                payment.getId(),
                isRefund ? null : command.qrVersion()
        );
    }

    public SouTerminalDataRequest toNet(Payment payment, PaymentCommand command) {
        LocalDateTime operationLocal = TimeZones.fromUtc(payment.getPaymentDate(), command.timeZoneId());
        LocalDate effectiveShiftDate = command.shiftDate() != null
                ? command.shiftDate()
                : operationLocal.toLocalDate();

        return new SouTerminalDataRequest(
                payment.getCashDeskUid(),
                TrxUtils.clearTrx(payment.getTrnId()),
                TimeZones.format(operationLocal),
                payment.getShiftNumber(),
                TimeZones.format(effectiveShiftDate.atStartOfDay()),
                command.operatorId(),
                null,
                command.deviceNo()
        );
    }

    public InvoiceConfirmRequest toNet(ConfirmCommand command) {
        return new InvoiceConfirmRequest(
                command.appId(),
                command.invoiceId(),
                command.souId(),
                command.payToolId(),
                command.success()
        );
    }

    public Sou toDomain(SouPaymentResponse response) {
        return new Sou(
                response.isSuccess(),
                response.errorCode(),
                response.errorMessage(),
                response.souTransaction(),
                response.slip(),
                response.operationTimeAsLocalDateTime()
        );
    }

    public Sou toDomain(InvoiceConfirmResponse response) {
        return new Sou(
                response.isSuccess(),
                response.errorCode() == null ? 0 : response.errorCode(),
                response.errorMessage(),
                null,
                null,
                TimeZones.parseSouOperationTime(response.operationTime())
        );
    }

    private List<SouGoodRequest> goods(Collection<Receipt> receipts, PaymentType type, String trnId, Payment payment) {
        List<SouGoodRequest> goods = new ArrayList<>();
        if (receipts != null && !receipts.isEmpty()) {
            receipts.forEach(receipt -> goods.add(good(receipt)));
        } else {
            goods.add(defaultGood(type, trnId, payment));
        }
        return goods;
    }

    private SouGoodRequest good(Receipt receipt) {
        return new SouGoodRequest(
                receipt.name(),
                receipt.amount(),
                null,
                zeroIfNull(receipt.discount()),
                null,
                null,
                null,
                Optional.ofNullable(receipt.quantity()).orElse(BigDecimal.ONE),
                Optional.ofNullable(receipt.barCode()).orElse("")
        );
    }

    private SouGoodRequest defaultGood(PaymentType type, String trnId, Payment payment) {
        return new SouGoodRequest(
                (type == PaymentType.REFUND ? "Возврат N " : "Оплата N ") + TrxUtils.clearTrx(trnId),
                payment.getAmount(),
                null,
                zeroIfNull(payment.getDiscount()),
                null,
                null,
                null,
                BigDecimal.ONE,
                ""
        );
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
