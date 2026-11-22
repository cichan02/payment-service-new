package com.besmart.qrpay.paymentservicenew.infra.persistence.mapper;

import com.besmart.qrpay.paymentservicenew.api.rest.to.response.PaymentDto;
import com.besmart.qrpay.paymentservicenew.api.rest.to.response.ReceiptItemDto;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.ReceiptEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;
import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public abstract class PaymentMapper {

    @Mapping(target = "money.amount", source = "amount")
    @Mapping(target = "money.discount", source = "discount")
    @Mapping(target = "money.currency", source = "currency")
    @Mapping(target = "money.currencyAbbr", source = "currencyAbbr")
    public abstract PaymentDto toResponse(PaymentEntity entity);

    @Mapping(target = "money.amount", source = "amount")
    @Mapping(target = "money.discount", source = "discount")
    @Mapping(target = "money.currency", source = "currency")
    @Mapping(target = "money.currencyAbbr", ignore = true)
    public abstract ReceiptItemDto toResponse(ReceiptEntity entity);

    public List<ReceiptItemDto> toResponses(Collection<ReceiptEntity> entities) {
        return entities.stream().map(this::toResponse).toList();
    }

    protected String mapPaymentType(PaymentType value) {
        return value == null ? null : value.name();
    }

    protected String mapPaymentState(PaymentState value) {
        return value == null ? null : value.name();
    }
}
