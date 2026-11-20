package com.besmart.qrpay.paymentservicenew;

import com.besmart.qrpay.paymentservicenew.SouStub.Outcome;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StornoRefundTest extends AbstractIntegrationTest {

    @Test
    void stornoBeforePaymentIsAppliedWhenPaymentArrives() {
        String terminal = "T-DELAYED";
        String trx = "trx-delayed";

        ResponseEntity<Map> early = post("/storno", stornoRequest(terminal, trx, 12.00));

        assertThat(early.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(early)).isEqualTo(-4004);
        assertThat(stateOf(terminal, trx + "-S")).isEqualTo("STORNO_PENDING");
        assertThat(SOU.count("storno")).isZero();

        ResponseEntity<Map> payment = post("/payment", paymentRequest(terminal, trx, 12.00));

        assertThat(payment.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(payment)).isEqualTo(-9295);
        assertThat(stateOf(terminal, trx)).isEqualTo("COMPLETED");
        assertThat(stateOf(terminal, trx + "-S")).isEqualTo("COMPLETED");
        assertThat(stornoFlag(terminal, trx)).isTrue();
        assertThat(SOU.count("payment")).isEqualTo(1);
        assertThat(SOU.count("storno")).isEqualTo(1);

        ResponseEntity<Map> duplicate = post("/payment", paymentRequest(terminal, trx, 12.00));

        assertThat(code(duplicate)).isEqualTo(-9295);
        assertThat(SOU.count("payment")).isEqualTo(1);
        assertThat(SOU.count("storno")).isEqualTo(1);
    }

    @Test
    void stornoAfterPaymentCancelsTheSaleAndIsIdempotent() {
        String terminal = "T-STORNO";
        String trx = "trx-storno";
        SOU.payment(Outcome.success(801));

        post("/payment", paymentRequest(terminal, trx, 20.00));
        ResponseEntity<Map> response = post("/storno", stornoRequest(terminal, trx, 20.00));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(response)).isZero();
        assertThat(stateOf(terminal, trx + "-S")).isEqualTo("COMPLETED");
        assertThat(successOf(terminal, trx + "-S")).isTrue();
        assertThat(stornoFlag(terminal, trx)).isTrue();
        assertThat(SOU.count("storno")).isEqualTo(1);

        ResponseEntity<Map> duplicate = post("/storno", stornoRequest(terminal, trx, 20.00));

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(duplicate)).isZero();
        assertThat(SOU.count("storno")).isEqualTo(1);
    }

    @Test
    void declinedStornoIsNotRetried() {
        String terminal = "T-STORNO-DECLINE";
        String trx = "trx-storno-decline";
        SOU.payment(Outcome.success(802));
        SOU.storno(Outcome.decline(-777, "Storno declined"));

        post("/payment", paymentRequest(terminal, trx, 25.00));
        ResponseEntity<Map> first = post("/storno", stornoRequest(terminal, trx, 25.00));

        assertThat(code(first)).isEqualTo(-777);
        assertThat(stateOf(terminal, trx + "-S")).isEqualTo("DECLINED");

        ResponseEntity<Map> second = post("/storno", stornoRequest(terminal, trx, 25.00));

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(second)).isEqualTo(-4404);
        assertThat(SOU.count("storno")).isEqualTo(1);
    }

    @Test
    void refundCreatesSuffixedRowAndIsIdempotent() throws Exception {
        String terminal = "T-REFUND";
        String trx = "trx-refund";
        SOU.payment(Outcome.success(810));
        SOU.refund(Outcome.success(811));

        post("/payment", paymentRequest(terminal, trx, 40.00));
        ResponseEntity<Map> response = post("/refund", refundRequest(terminal, trx, 40.00, 810));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(code(response)).isZero();
        assertThat(stateOf(terminal, trx + "-R")).isEqualTo("COMPLETED");
        assertThat(((Number) data(response.getBody(), "payment").get("souId")).longValue())
                .isEqualTo(811);

        Map<String, Object> sent = mapper.convertValue(
                mapper.readTree(SOU.last("refund").body()), Map.class);
        assertThat(((Number) sent.get("souTransaction")).longValue()).isEqualTo(810);

        ResponseEntity<Map> duplicate = post("/refund", refundRequest(terminal, trx, 40.00, 810));

        assertThat(code(duplicate)).isZero();
        assertThat(SOU.count("refund")).isEqualTo(1);
    }

    @Test
    void refundOfUnknownPaymentIsRejected() {
        ResponseEntity<Map> response = post("/refund",
                refundRequest("T-REFUND-MISS", "trx-refund-miss", 5.00, 999_999));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(code(response)).isEqualTo(-4403);
        assertThat(SOU.count("refund")).isZero();
    }

    private int code(ResponseEntity<Map> response) {
        return ((Number) response.getBody().get("errorCode")).intValue();
    }

    private boolean stornoFlag(String terminalId, String trnId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select is_storno from public.payment where cash_desk_uid = ? and trn_id = ?",
                Boolean.class, terminalId, trnId));
    }
}
