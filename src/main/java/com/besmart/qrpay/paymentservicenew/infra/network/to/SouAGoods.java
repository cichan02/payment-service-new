package com.besmart.qrpay.paymentservicenew.infra.network.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SouAGoods {

    @JsonProperty("goods")
    private List<SouGood> goods;
}
