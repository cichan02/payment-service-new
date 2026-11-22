package com.besmart.qrpay.paymentservicenew.infra.persistence.repository;

import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.ReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReceiptRepository extends JpaRepository<ReceiptEntity, Long> {

    List<ReceiptEntity> findByPaymentIdOrderByIdAsc(Long paymentId);

    List<ReceiptEntity> findByPaymentIdInOrderByIdAsc(Collection<Long> paymentIds);
}
