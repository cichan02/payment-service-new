package com.besmart.qrpay.paymentservicenew.infra.persistence;

import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.ApplicationError;
import com.besmart.qrpay.paymentservicenew.core.domain.exception.BusinessException;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Money;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Payment;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.ReceiptEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.mapper.PaymentDbMapper;
import com.besmart.qrpay.paymentservicenew.infra.persistence.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PaymentStore {

    private final ReceiptRepository receiptRepository;
    private final PaymentDbMapper dbMapper;

    public String lockKey(String terminalId, String baseTrxId) {
        return terminalId + "|" + baseTrxId;
    }

    public void saveReceipts(Long paymentId, Collection<Receipt> items, Money money, String trxId) {
        List<ReceiptEntity> receipts = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            ReceiptEntity receipt = new ReceiptEntity();
            receipt.setPaymentId(paymentId);
            receipt.setName("Оплата N " + trxId);
            receipt.setPrice(BigDecimal.ZERO);
            receipt.setCurrency(money.currency());
            receipt.setQuantity(BigDecimal.ONE);
            receipt.setUnitName("");
            receipt.setAmount(money.amount());
            receipt.setDiscount(money.discount());
            receipt.setBarCode(null);
            receipts.add(receipt);
        } else {
            for (ReceiptEntity receipt : dbMapper.toEntities(items)) {
                receipt.setPaymentId(paymentId);
                receipts.add(receipt);
            }
        }
        receiptRepository.saveAll(receipts);
    }

    /**
     * A retry must describe the same transaction. A different amount, discount or
     * currency for the same terminal+trx usually means a cash-desk bug or a trx
     * collision and must not overwrite the original row.
     */
    public void assertSameTransaction(Payment existing, Money money) {
        boolean sameAmount = existing.getAmount() != null
                && money.amount() != null
                && existing.getAmount().compareTo(money.amount()) == 0;
        boolean sameDiscount = (existing.getDiscount() == null ? BigDecimal.ZERO : existing.getDiscount())
                .compareTo(money.discount() == null ? BigDecimal.ZERO : money.discount()) == 0;
        boolean sameCurrency = Objects.equals(existing.getCurrency(), money.currency());

        if (!sameAmount || !sameDiscount || !sameCurrency) {
            throw new BusinessException(ApplicationError.IDEMPOTENCY_CONFLICT, HttpStatus.CONFLICT,
                    "The transaction " + existing.getTrnId() + " already exists with a different amount, "
                            + "discount or currency");
        }
    }

    public Collection<Receipt> receipts(Long paymentId) {
        return dbMapper.toDomains(receiptRepository.findByPaymentIdOrderByIdAsc(paymentId));
    }
}
