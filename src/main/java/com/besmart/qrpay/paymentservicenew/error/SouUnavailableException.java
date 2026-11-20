package com.besmart.qrpay.paymentservicenew.error;

import org.springframework.http.HttpStatus;

/**
 * SOU is unreachable or timed out. This is not a decline: the transaction stays in
 * CREATED and the caller may retry the same request.
 */
public class SouUnavailableException extends BusinessException {

    public SouUnavailableException(String message, Throwable cause) {
        super(ApplicationError.SOU_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, message);
        initCause(cause);
    }

    public SouUnavailableException(String message) {
        super(ApplicationError.SOU_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
