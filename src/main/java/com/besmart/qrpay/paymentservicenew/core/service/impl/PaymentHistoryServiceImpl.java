package com.besmart.qrpay.paymentservicenew.core.service.impl;

import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.ApplicationError;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.BusinessException;
import com.besmart.qrpay.paymentservicenew.core.domain.model.PaymentHistory;
import com.besmart.qrpay.paymentservicenew.core.domain.model.PaymentHistoryItem;
import com.besmart.qrpay.paymentservicenew.core.domain.model.PaymentHistoryQuery;
import com.besmart.qrpay.paymentservicenew.core.service.PaymentHistoryService;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.ReceiptEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.mapper.PaymentDbMapper;
import com.besmart.qrpay.paymentservicenew.infra.persistence.repository.PaymentRepository;
import com.besmart.qrpay.paymentservicenew.infra.persistence.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Payment history for mobile-api. Returns the persisted Payment + Receipt rows; no
 * {@code payments_view} and no retail/store enrichment. Enrichment stays in mobile-api.
 */
@Service
@RequiredArgsConstructor
public class PaymentHistoryServiceImpl implements PaymentHistoryService {

    private final PaymentRepository payments;
    private final ReceiptRepository receipts;
    private final PaymentDbMapper dbMapper;
    private final Clock clock;

    @Override
    public PaymentHistory history(PaymentHistoryQuery query) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (query.dateFrom() > query.dateTo()) {
            throw new BusinessException(ApplicationError.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "dateFrom must be less than or equal to dateTo");
        }

        LocalDateTime from = LocalDateTime.ofInstant(Instant.ofEpochMilli(query.dateFrom()), ZoneOffset.UTC);
        LocalDateTime to = LocalDateTime.ofInstant(Instant.ofEpochMilli(query.dateTo()), ZoneOffset.UTC);

        Specification<PaymentEntity> specification = (root, ignored, cb) -> cb.and(
                cb.equal(root.get("qrPayClientId"), query.qrPayClientId()),
                cb.between(root.get("paymentDate"), from, to)
        );
        if (query.payToolId() != null) {
            specification = specification.and((root, ignored, cb) ->
                    cb.equal(root.get("payToolId"), query.payToolId()));
        }
        if (query.cardNumber() != null && !query.cardNumber().isBlank()) {
            specification = specification.and((root, ignored, cb) ->
                    cb.equal(root.get("smartPaymentCardNumber"), query.cardNumber()));
        }

        List<PaymentEntity> found = payments.findAll(
                specification,
                Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id")));
        if (found.isEmpty()) {
            return new PaymentHistory(now, List.of());
        }

        List<Long> ids = found.stream().map(PaymentEntity::getId).toList();
        Map<Long, List<ReceiptEntity>> byPayment = receipts.findByPaymentIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(ReceiptEntity::getPaymentId));

        List<PaymentHistoryItem> entries = found.stream()
                .map(payment -> new PaymentHistoryItem(
                        dbMapper.toDomain(payment),
                        dbMapper.toDomains(byPayment.getOrDefault(payment.getId(), List.of()))))
                .toList();
        return new PaymentHistory(now, entries);
    }
}
