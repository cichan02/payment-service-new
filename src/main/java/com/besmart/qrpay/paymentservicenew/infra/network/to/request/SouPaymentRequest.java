package com.besmart.qrpay.paymentservicenew.infra.network.to.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record SouPaymentRequest(
        SouTerminalDataRequest souTerminalData,
        BigDecimal amount,
        BigDecimal discount,
        @JsonProperty("goods") List<SouGoodRequest> goods,
        Short currency,
        Long souId,
        Short validitySecond,
        String mobileQueue,
        String smartPaymentCardNumber,
        String loyaltyJson,
        Long souTransaction,
        Long payToolId,
        Long cashDeskApiTrxId,
        String qrVersion
) {
}
