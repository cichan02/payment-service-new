package com.besmart.qrpay.paymentservice.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Error codes are part of the wire contract. Codes -1300, -4004 and -9295 keep the
 * values already used by cash-desk-api/mobile-api; the rest are new.
 */
@Getter
@RequiredArgsConstructor
public enum ApplicationError {

    SUCCESS("", 0),
    GENERIC_PAYMENT_SERVICE_ERROR("Payment service internal error", -4400),
    VALIDATION_ERROR("Validation error", -4401),
    IDEMPOTENCY_CONFLICT("Request conflicts with an existing transaction", -4402),
    ORIGINAL_PAYMENT_NOT_FOUND("Original payment not found", -4403),
    PAYMENT_DECLINED("Payment was declined", -4404),
    PAYMENT_NOT_FOUND("Payment not found", -4405),
    SALE_TRANSACTION_NOT_FOUND("Sale transaction not found", -4004),
    SOU_SERVICE_UNAVAILABLE("Sou service unavailable", -1300),
    PAYMENT_CANCELLED("Payment is cancelled by CashDesk", -9295);

    private final String message;
    private final int errorCode;
}
