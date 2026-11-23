package com.besmart.qrpay.paymentservicenew.api.rest.mapper;

import com.besmart.qrpay.paymentservicenew.api.rest.to.request.ConfirmRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.PaymentHistoryRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.PaymentRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.ReceiptItemRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.RefundRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.request.StornoRequest;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.PaymentHistoryItem;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.PaymentHistoryResponse;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.PaymentResponse;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.ReceiptItemResponse;
import com.besmart.qrpay.paymentservicenew.core.domain.command.ConfirmCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.command.PaymentCommand;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;
import com.besmart.qrpay.paymentservicenew.core.domain.model.PaymentHistory;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;
import com.besmart.qrpay.paymentservicenew.core.domain.query.PaymentHistoryQuery;
import com.besmart.qrpay.paymentservicenew.core.domain.result.OperationResult;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public abstract class PaymentRestMapper {

    @Mapping(target = "originalSouTransactionId", ignore = true)
    public abstract PaymentCommand toDomain(PaymentRequest request);

    @Mapping(target = "souId", ignore = true)
    @Mapping(target = "mobileQueue", ignore = true)
    @Mapping(target = "qrVersion", ignore = true)
    @Mapping(target = "loyaltyJson", ignore = true)
    @Mapping(target = "smartPaymentCardNumber", ignore = true)
    public abstract PaymentCommand toDomain(RefundRequest request);

    @Mapping(target = "souId", ignore = true)
    @Mapping(target = "mobileQueue", ignore = true)
    @Mapping(target = "qrVersion", ignore = true)
    @Mapping(target = "loyaltyJson", ignore = true)
    @Mapping(target = "originalSouTransactionId", ignore = true)
    @Mapping(target = "payToolId", ignore = true)
    @Mapping(target = "smartPaymentCardNumber", ignore = true)
    public abstract PaymentCommand toDomain(StornoRequest request);

    public abstract ConfirmCommand toDomain(ConfirmRequest request);

    public abstract PaymentHistoryQuery toDomain(PaymentHistoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "amount", source = "money.amount")
    @Mapping(target = "discount", source = "money.discount")
    @Mapping(target = "currency", source = "money.currency")
    @Mapping(target = "quantity", source = "quantity", defaultValue = "1")
    public abstract Receipt toDomain(ReceiptItemRequest request);

    @Mapping(target = "money.amount", source = "amount")
    @Mapping(target = "money.discount", source = "discount")
    @Mapping(target = "money.currency", source = "currency")
    @Mapping(target = "money.currencyAbbr", source = "currencyAbbr")
    public abstract PaymentResponse toResponse(Payment payment);

    @Mapping(target = "money.amount", source = "amount")
    @Mapping(target = "money.discount", source = "discount")
    @Mapping(target = "money.currency", source = "currency")
    @Mapping(target = "money.currencyAbbr", ignore = true)
    public abstract ReceiptItemResponse toResponse(Receipt receipt);

    public abstract Collection<ReceiptItemResponse> toResponses(Collection<Receipt> receipts);

    /**
     * A slip is a multi-line text block; the wire contract wants one list item per
     * line, and no list at all when SOU did not return a slip.
     */
    public OperationResponse toResponse(OperationResult result) {
        return new OperationResponse(
                result.errorCode(),
                result.errorMessage(),
                result.operationTime(),
                result.trxId(),
                result.operationId(),
                result.slip(),
                splitSlip(result.slip()),
                result.payment() == null ? null : toResponse(result.payment()),
                result.receipts() == null ? null : List.copyOf(toResponses(result.receipts()))
        );
    }

    public PaymentHistoryResponse toResponse(PaymentHistory history) {
        List<PaymentHistoryItem> payments = history.entries().stream()
                .map(entry -> new PaymentHistoryItem(
                        toResponse(entry.payment()),
                        List.copyOf(toResponses(entry.receipts()))))
                .toList();
        return new PaymentHistoryResponse(0, "", history.operationTime(), payments);
    }

    protected String mapPaymentType(PaymentType value) {
        return value == null ? null : value.name();
    }

    protected String mapPaymentState(PaymentState value) {
        return value == null ? null : value.name();
    }

    private List<String> splitSlip(String slip) {
        if (slip == null || slip.isBlank()) {
            return null;
        }
        return Arrays.asList(slip.split("\\R"));
    }
}
