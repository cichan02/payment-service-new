package com.besmart.qrpay.paymentservice;

import com.besmart.qrpay.paymentservice.SouStub.Outcome;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmHistoryTest extends AbstractIntegrationTest {

    @Test
    void confirmMarksPaymentAndDuplicateIsNotSentToSou() {
        String terminal = "T-CONFIRM";
        String trx = "trx-confirm";
        SOU.payment(Outcome.success(820));

        post("/payment", paymentRequest(terminal, trx, 18.00, 77L));
        assertThat(stateOf(terminal, trx)).isEqualTo("COMPLETED");

        ResponseEntity<Map> first = post("/confirm", confirmRequest(1, 77, 77, trx, true));

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(first)).isZero();
        assertThat(stateOf(terminal, trx)).isEqualTo("CONFIRMED");
        // /confirm must not own the success flag.
        assertThat(successOf(terminal, trx)).isTrue();
        assertThat(SOU.count("invoicePayment")).isEqualTo(1);

        ResponseEntity<Map> duplicate = post("/confirm", confirmRequest(1, 77, 77, trx, true));

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(duplicate)).isZero();
        assertThat(SOU.count("invoicePayment")).isEqualTo(1);
    }

    @Test
    void confirmDoesNotSetSuccessForUnsuccessfulPayment() {
        String terminal = "T-CONFIRM-FAIL";
        String trx = "trx-confirm-fail";
        SOU.payment(Outcome.decline(-500, "No money"));

        post("/payment", paymentRequest(terminal, trx, 9.00, 78L));
        ResponseEntity<Map> response = post("/confirm", confirmRequest(2, 78, 78, trx, true));

        assertThat(code(response)).isZero();
        assertThat(stateOf(terminal, trx)).isEqualTo("CONFIRMED");
        assertThat(successOf(terminal, trx)).isFalse();
    }

    @Test
    void cancelWithoutTrxIdStillCallsSou() {
        Map<String, Object> request = new LinkedHashMap<>(
                confirmRequest(3, 79, 79, null, false));

        ResponseEntity<Map> response = post("/confirm", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(response)).isZero();
        assertThat(SOU.count("invoicePayment")).isEqualTo(1);
    }

    @Test
    void historyReturnsPaymentsWithReceipts() {
        SOU.payment(Outcome.success(830));
        post("/payment", paymentRequest("T-HISTORY", "trx-h1", 11.00, 777L));
        SOU.payment(Outcome.success(831));
        post("/payment", paymentRequest("T-HISTORY", "trx-h2", 22.00, 777L));
        // Another client must not leak into the result.
        post("/payment", paymentRequest("T-HISTORY", "trx-h3", 33.00, 778L));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("qrPayClientId", 777);
        request.put("dateFrom", 0);
        request.put("dateTo", 4_102_444_800_000L);

        ResponseEntity<Map> response = post("/payment-history", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(response)).isZero();
        List<?> payments = (List<?>) response.getBody().get("payments");
        assertThat(payments).hasSize(2);
        Map<?, ?> first = (Map<?, ?>) payments.get(0);
        assertThat((List<?>) first.get("receipts")).hasSize(1);
        assertThat(((Map<?, ?>) first.get("payment")).get("trnId")).isEqualTo("trx-h2");
    }

    @Test
    void historyRejectsInvertedDateRange() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("qrPayClientId", 777);
        request.put("dateFrom", 2_000_000_000_000L);
        request.put("dateTo", 1_000_000_000_000L);

        ResponseEntity<Map> response = post("/payment-history", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(code(response)).isEqualTo(-4401);
    }

    private int code(ResponseEntity<Map> response) {
        return ((Number) response.getBody().get("errorCode")).intValue();
    }
}
