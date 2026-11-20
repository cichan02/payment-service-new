package com.besmart.qrpay.paymentservicenew.repository;

import com.besmart.qrpay.paymentservicenew.domain.PaymentType;
import com.besmart.qrpay.paymentservicenew.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long>,
        JpaSpecificationExecutor<PaymentEntity> {

    Optional<PaymentEntity> findByCashDeskUidAndTrnIdAndPaymentType(
            String cashDeskUid, String trnId, PaymentType paymentType);

    Optional<PaymentEntity> findFirstBySouIdOrderByIdAsc(Long souId);

    Optional<PaymentEntity> findFirstByQrPayClientIdAndTrnIdAndPaymentTypeOrderByIdDesc(
            Long qrPayClientId, String trnId, PaymentType paymentType);

    /**
     * Serializes all commands for one terminal transaction. The lock is released when
     * the surrounding transaction commits or rolls back.
     */
    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void acquireTransactionLock(@Param("key") String key);

    /**
     * Row-level mutex for the SOU call. It is held for the duration of the transaction,
     * so a concurrent duplicate waits and then sees the final state instead of calling
     * SOU twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.id = :id")
    Optional<PaymentEntity> findByIdForUpdate(@Param("id") Long id);
}
