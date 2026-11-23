package com.besmart.qrpay.paymentservicenew.api.rest.mapper;

import com.besmart.qrpay.paymentservicenew.api.rest.to.response.OperationResponse;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.ApplicationError;
import com.besmart.qrpay.paymentservicenew.core.domain.enumeration.PaymentState;
import com.besmart.qrpay.paymentservicenew.infra.network.to.SouPaymentResponse;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.PaymentEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.entity.ReceiptEntity;
import com.besmart.qrpay.paymentservicenew.infra.persistence.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ResponseFactory {

    private final PaymentMapper paymentMapper;

    public OperationResponse error(ApplicationError error, String message, LocalDateTime operationTime) {
        return new OperationResponse(
        		error.getErrorCode(),
          		message,
            	operationTime,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public OperationResponse of(
    	PaymentEntity payment,
     	List<ReceiptEntity> receipts,
     	SouPaymentResponse souResponse,
      	LocalDateTime operationTime
    ) {
        String slip = souResponse == null ? null : souResponse.getSlip();
        return new OperationResponse(
                souResponse == null ? 0 : souResponse.getErrorCode(),
                souResponse == null ? "" : souResponse.getErrorMessage(),
                operationTime,
                payment.getTrnId(),
                souResponse == null ? payment.getSouId() : souResponse.getSouTransaction(),
                slip,
                splitSlip(slip),
                paymentMapper.toResponse(payment),
                paymentMapper.toResponses(receipts)
        );
    }

    /**
     * Response for a duplicate request whose transaction is already final. The original
     * SOU response is not stored, so the error code is derived from the persisted state.
     */
    public OperationResponse stored(PaymentEntity payment, List<ReceiptEntity> receipts,
                                    LocalDateTime operationTime) {
        int errorCode = 0;
        String errorMessage = "";

        if (payment.getState() == PaymentState.CANCELLED) {
            errorCode = ApplicationError.PAYMENT_CANCELLED.getErrorCode();
            errorMessage = ApplicationError.PAYMENT_CANCELLED.getMessage();
        } else if (!payment.isSuccess() || payment.getState() == PaymentState.DECLINED
                || payment.getState() == PaymentState.LEGACY) {
            errorCode = ApplicationError.PAYMENT_DECLINED.getErrorCode();
            errorMessage = payment.getErrorText() == null
                    ? ApplicationError.PAYMENT_DECLINED.getMessage()
                    : payment.getErrorText();
        }

        return new OperationResponse(
                errorCode,
                errorMessage,
                operationTime,
                payment.getTrnId(),
                payment.getSouId(),
                null,
                null,
                paymentMapper.toResponse(payment),
                paymentMapper.toResponses(receipts)
        );
    }

    private List<String> splitSlip(String slip) {
        if (slip == null || slip.isBlank()) {
            return null;
        }
        return Arrays.asList(slip.split("\\R"));
    }
}
