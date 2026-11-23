package com.besmart.qrpay.paymentservicenew.infra.persistence.mapper;

import com.besmart.qrpay.paymentservicenew.core.domain.Receipt;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;ceipt;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.ReceiptEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public abstract class PaymentDbMapper {

    public abstract Payment toDomain(PaymentEntity entity);

    public abstract PaymentEntity toEntity(Payment payment);

    public abstract Receipt toDomain(ReceiptEntity entity);

    public abstract Collection<Receipt> toDomains(Collection<ReceiptEntity> entities);

    public abstract ReceiptEntity toEntity(Receipt receipt);

    public abstract Collection<ReceiptEntity> toEntities(Collection<Receipt> receipts);
}
