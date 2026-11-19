package com.besmart.qrpay.paymentservice.sou.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class SouGood {

    private String productName;
    private BigDecimal amount;
    private Short currency;
    private BigDecimal discount;
    private String description;
    private BigDecimal price;
    private String unitName;
    private BigDecimal quantity;
    private String barcode;
}
