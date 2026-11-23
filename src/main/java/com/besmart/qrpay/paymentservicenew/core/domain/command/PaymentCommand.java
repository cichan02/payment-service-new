package com.besmart.qrpay.paymentservicenew.core.domain.command;

import java.time.LocalDate;
import java.util.Collection;

import com.besmart.qrpay.paymentservicenew.core.domain.model.Money;
import com.besmart.qrpay.paymentservicenew.core.domain.model.Receipt;

/**
 * Operation context for a sale, refund or storno. Cash-desk-api has already resolved
 * terminal, client, SOU id, pay tool and store time zone before the call, so the
 * command is the complete domain input of a money-moving operation.
 *
 * <p>{@code operationDateTime} is intentionally absent: the service generates it.</p>
 */
public record PaymentCommand(
        String terminalId,
        Long retailId,
        String trxId,
        Integer shiftNo,
        LocalDate shiftDate,
        Integer receiptNumber,
        Money money,
        Long qrPayClientId,
        Long souId,
        Long payToolId,
        String smartPaymentCardNumber,
        String operatorId,
        String operatorName,
        String deviceNo,
        String timeZoneId,
        String terminalIp,
        String mobileQueue,
        String qrVersion,
        String loyaltyJson,
        Collection<Receipt> receipts,
        Long originalSouTransactionId
) {
}
