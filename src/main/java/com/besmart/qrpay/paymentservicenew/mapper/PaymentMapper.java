package com.besmart.qrpay.paymentservicenew.mapper;

import com.besmart.qrpay.paymentservicenew.domain.MoneyDto;
import com.besmart.qrpay.paymentservicenew.domain.PaymentDto;
import com.besmart.qrpay.paymentservicenew.domain.ReceiptItemDto;
import com.besmart.qrpay.paymentservicenew.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.entity.ReceiptEntity;

import java.math.BigDecimal;
import java.util.List;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentDto toDto(PaymentEntity payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getCashDeskUid(),
                payment.getTrnId(),
                payment.getPaymentDate(),
                new MoneyDto(payment.getAmount(), payment.getDiscount(),
                        payment.getCurrency(), payment.getCurrencyAbbr()),
                payment.isSuccess(),
                payment.getSouId(),
                payment.getPaymentType() == null ? null : payment.getPaymentType().name(),
                payment.getShiftNumber(),
                payment.getReceiptNumber(),
                payment.getQrPayClientId(),
                payment.isStorno(),
                payment.getState() == null ? null : payment.getState().name(),
                payment.getTerminalIp(),
                payment.getErrorText(),
                payment.getPayToolId(),
                payment.getSmartPaymentCardNumber()
        );
    }

    public static ReceiptItemDto toDto(ReceiptEntity receipt) {
        return new ReceiptItemDto(
                receipt.getId(),
                receipt.getPaymentId(),
                receipt.getName(),
                receipt.getPrice(),
                receipt.getQuantity(),
                receipt.getUnitName(),
                receipt.getBarCode(),
                new MoneyDto(receipt.getAmount(), receipt.getDiscount(),
                        receipt.getCurrency(), null)
        );
    }

    public static List<ReceiptItemDto> toReceiptDtos(List<ReceiptEntity> receipts) {
        return receipts.stream().map(PaymentMapper::toDto).toList();
    }

    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
