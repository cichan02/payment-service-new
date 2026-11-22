package com.besmart.qrpay.paymentservicenew.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "receipt", schema = "public")
public class ReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "currency")
    private Integer currency;

    @Column(name = "quantity", precision = 19, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_name", length = 20)
    private String unitName;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount", precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(name = "bar_code", length = 20)
    private String barCode;
}
