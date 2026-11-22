package com.besmart.qrpay.paymentservicenew.api.rest.to.request;

import com.besmart.qrpay.paymentservicenew.api.rest.to.common.MoneyDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ReceiptItemRequest(
        @NotBlank String name,
        @DecimalMin("0.00") BigDecimal price,
        @NotNull @Valid MoneyDto money,
        @Positive BigDecimal quantity,
        String unitName,
        String barCode
) {
}
