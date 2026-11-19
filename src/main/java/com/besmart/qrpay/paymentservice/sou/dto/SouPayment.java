package com.besmart.qrpay.paymentservice.sou.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class SouPayment {

    private SouTerminalData souTerminalData;
    private BigDecimal amount;
    private BigDecimal discount;

    @JsonUnwrapped
    private SouAGoods products;

    private Short currency;
    private Long souId;
    private Short validitySecond;
    private String mobileQueue;
    private String smartPaymentCardNumber;
    private String loyaltyJson;

    /** Refund only: SOU transaction number of the payment being refunded. */
    private Long souTransaction;
    private Long payToolId;
    private Long cashDeskApiTrxId;
    private String qrVersion;
}
