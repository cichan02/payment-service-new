package com.besmart.qrpay.paymentservice.service;

import com.besmart.qrpay.paymentservice.api.dto.PaymentHistoryRequest;
import com.besmart.qrpay.paymentservice.domain.PaymentHistoryItem;
import com.besmart.qrpay.paymentservice.domain.PaymentHistoryResponse;
import com.besmart.qrpay.paymentservice.entity.PaymentEntity;
import com.besmart.qrpay.paymentservice.entity.ReceiptEntity;
import com.besmart.qrpay.paymentservice.error.ApplicationError;
import com.besmart.qrpay.paymentservice.error.BusinessException;
import com.besmart.qrpay.paymentservice.mapper.PaymentMapper;
import com.besmart.qrpay.paymentservice.repository.PaymentRepository;
import com.besmart.qrpay.paymentservice.repository.ReceiptRepository;
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
public class PaymentHistoryService {

    private final PaymentRepository payments;
    private final ReceiptRepository receipts;
    private final Clock clock;

    public PaymentHistoryResponse history(PaymentHistoryRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (request.dateFrom() > request.dateTo()) {
            throw new BusinessException(ApplicationError.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "dateFrom must be less than or equal to dateTo");
        }

        LocalDateTime from = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.dateFrom()), ZoneOffset.UTC);
        LocalDateTime to = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.dateTo()), ZoneOffset.UTC);

        Specification<PaymentEntity> specification = (root, query, cb) -> cb.and(
                cb.equal(root.get("qrPayClientId"), request.qrPayClientId()),
                cb.between(root.get("paymentDate"), from, to)
        );
        if (request.payToolId() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("payToolId"), request.payToolId()));
        }
        if (request.cardNumber() != null && !request.cardNumber().isBlank()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("smartPaymentCardNumber"), request.cardNumber()));
        }

        List<PaymentEntity> found = payments.findAll(
                specification,
                Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id")));
        if (found.isEmpty()) {
            return new PaymentHistoryResponse(0, "", now, List.of());
        }

        List<Long> ids = found.stream().map(PaymentEntity::getId).toList();
        Map<Long, List<ReceiptEntity>> byPayment = receipts.findByPaymentIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(ReceiptEntity::getPaymentId));

        List<PaymentHistoryItem> items = found.stream()
                .map(payment -> new PaymentHistoryItem(
                        PaymentMapper.toDto(payment),
                        PaymentMapper.toReceiptDtos(byPayment.getOrDefault(payment.getId(), List.of()))))
                .toList();
        return new PaymentHistoryResponse(0, "", now, items);
    }
}
