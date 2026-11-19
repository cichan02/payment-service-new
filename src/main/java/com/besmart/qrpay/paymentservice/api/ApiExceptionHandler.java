package com.besmart.qrpay.paymentservice.api;

import com.besmart.qrpay.paymentservice.domain.ErrorResponse;
import com.besmart.qrpay.paymentservice.error.ApplicationError;
import com.besmart.qrpay.paymentservice.error.BusinessException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Business error {}: {}", ex.getError(), ex.getMessage(), ex);
        } else {
            log.warn("Business error {}: {}", ex.getError(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getError().getErrorCode(), ex.getMessage(), now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(
                ApplicationError.VALIDATION_ERROR.getErrorCode(),
                message.isBlank() ? ApplicationError.VALIDATION_ERROR.getMessage() : message,
                now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                ApplicationError.VALIDATION_ERROR.getErrorCode(),
                "Malformed JSON request",
                now()));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> feign(FeignException ex) {
        log.error("Unexpected SOU failure", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(
                ApplicationError.SOU_SERVICE_UNAVAILABLE.getErrorCode(),
                ApplicationError.SOU_SERVICE_UNAVAILABLE.getMessage(),
                now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Unexpected payment service error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                ApplicationError.GENERIC_PAYMENT_SERVICE_ERROR.getErrorCode(),
                ApplicationError.GENERIC_PAYMENT_SERVICE_ERROR.getMessage(),
                now()));
    }

    private String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
