package com.besmart.qrpay.paymentservicenew.core.domain;

import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment aggregate root. Mutable on purpose: services load it inside a transaction,
 * let it transition and persist it back. All state transitions go through the
 * behaviour methods below, so the service never sets {@code state} or {@code success}
 * by hand.
 */
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    private Long id;
    private String cashDeskUid;
    private String trnId;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private BigDecimal discount = BigDecimal.ZERO;
    private boolean success;
    private Long souId;
    private PaymentType paymentType;
    private Integer shiftNumber;
    private Integer currency;
    private String receiptNumber;
    private Long qrPayClientId;
    private String currencyAbbr;
    private boolean storno;
    private String terminalIp;
    private String qrPayClientIp;
    private String errorText;
    private Long payToolId;
    private String smartPaymentCardNumber;
    private PaymentState state = PaymentState.CREATED;

    public Money money() {
        return new Money(amount, discount, currency, currencyAbbr);
    }

    public void applyMoney(Money money) {
        this.amount = money.amount();
        this.discount = money.discount();
        this.currency = money.currency();
        this.currencyAbbr = money.currencyAbbr();
    }

    /**
     * A settled row must never be sent to SOU again: the remote side may already have
     * applied it and a retry could move money twice.
     */
    public boolean isFinal() {
        return state.isSettled();
    }

    public void complete(Long souId, LocalDateTime paymentDate) {
        this.state = PaymentState.COMPLETED;
        this.success = true;
        this.souId = souId == null ? 0L : souId;
        if (paymentDate != null) {
            this.paymentDate = paymentDate;
        }
    }

    public void decline(String errorMessage) {
        this.state = PaymentState.DECLINED;
        this.success = false;
        this.errorText = errorMessage;
    }

    public void confirm() {
        this.state = PaymentState.CONFIRMED;
    }

    public void cancel() {
        this.state = PaymentState.CANCELLED;
    }

    public void error(String errorMessage) {
        this.errorText = errorMessage;
    }
}
