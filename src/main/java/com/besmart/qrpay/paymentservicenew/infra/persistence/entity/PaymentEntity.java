package com.besmart.qrpay.paymentservicenew.infra.persistence.entity;

import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Existing shared table {@code public.payment}. The new service only adds and reads
 * columns it owns; {@code bic} is deliberately not mapped because it is resolved by
 * cash-desk-api after the payment response.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment", schema = "public")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "cash_desk_uid", nullable = false, length = 40)
    private String cashDeskUid;

    @Column(name = "trn_id", nullable = false, length = 50)
    private String trnId;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "sou_id")
    private Long souId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 10)
    private PaymentType paymentType;

    @Column(name = "shift_number")
    private Integer shiftNumber;

    @Column(name = "currency")
    private Integer currency;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "qrpay_client_id")
    private Long qrPayClientId;

    @Column(name = "currency_abbr", length = 3)
    private String currencyAbbr;

    @Column(name = "is_storno")
    private boolean storno;

    @Column(name = "terminal_ip")
    private String terminalIp;

    @Column(name = "qrpay_client_ip", length = 20)
    private String qrPayClientIp;

    @Column(name = "error_text")
    private String errorText;

    @Column(name = "pay_tool_id")
    private Long payToolId;

    @Column(name = "smart_payment_card_number", length = 20)
    private String smartPaymentCardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private PaymentState state = PaymentState.CREATED;
}
