package com.besmart.qrpay.paymentservicenew.core.domain.exception;

import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.ApplicationError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final ApplicationError error;
    private final HttpStatus status;

    public BusinessException(ApplicationError error, HttpStatus status) {
        this(error, status, error.getMessage());
    }

    public BusinessException(ApplicationError error, HttpStatus status, String message) {
        super(message);
        this.error = error;
        this.status = status;
    }
}
