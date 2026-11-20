package com.besmart.qrpay.paymentservicenew;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.4"))
                    .withDatabaseName("smartpay")
                    .withUsername("smartpay")
                    .withPassword("smartpay");

    protected static final SouStub SOU = SouStub.start();

    static {
        POSTGRES.start();
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper mapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations",
                () -> "classpath:db/test-migration,classpath:db/migration");
        registry.add("sou.url", () -> "http://127.0.0.1:" + SOU.port());
    }

    @BeforeEach
    void resetSou() {
        SOU.reset();
    }

    protected ResponseEntity<Map> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(
                "http://127.0.0.1:" + port + "/api/v1/payment-new" + path,
                new HttpEntity<>(body, headers),
                Map.class);
    }

    protected Map<String, Object> money(double amount, double discount) {
        Map<String, Object> money = new LinkedHashMap<>();
        money.put("amount", amount);
        money.put("discount", discount);
        money.put("currency", 933);
        money.put("currencyAbbr", "BYN");
        return money;
    }

    protected Map<String, Object> paymentRequest(String terminalId, String trxId, double amount) {
        return paymentRequest(terminalId, trxId, amount, 7L);
    }

    protected Map<String, Object> paymentRequest(String terminalId, String trxId, double amount,
                                                 Long qrPayClientId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("terminalId", terminalId);
        request.put("retailId", 1);
        request.put("trxId", trxId);
        request.put("shiftNo", 1);
        request.put("shiftDate", LocalDate.now().toString());
        request.put("receiptNumber", 10);
        request.put("money", money(amount, 0));
        request.put("qrPayClientId", qrPayClientId);
        request.put("timeZoneId", "Europe/Minsk");
        request.put("operatorId", "op-1");
        request.put("operatorName", "Operator");
        request.put("deviceNo", "device-1");
        request.put("terminalIp", "10.0.0.1");
        request.put("receipts", List.of(receipt("Item", amount)));
        return request;
    }

    protected Map<String, Object> receipt(String name, double amount) {
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("name", name);
        receipt.put("price", amount);
        receipt.put("money", money(amount, 0));
        receipt.put("quantity", 1);
        receipt.put("unitName", "pcs");
        receipt.put("barCode", "123");
        return receipt;
    }

    protected Map<String, Object> stornoRequest(String terminalId, String trxId, double amount) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("terminalId", terminalId);
        request.put("retailId", 1);
        request.put("trxId", trxId);
        request.put("shiftNo", 1);
        request.put("shiftDate", LocalDate.now().toString());
        request.put("receiptNumber", 10);
        request.put("money", money(amount, 0));
        request.put("qrPayClientId", 7L);
        request.put("timeZoneId", "Europe/Minsk");
        request.put("operatorId", "op-1");
        request.put("deviceNo", "device-1");
        request.put("terminalIp", "10.0.0.1");
        request.put("receipts", List.of(receipt("Item", amount)));
        return request;
    }

    protected Map<String, Object> refundRequest(String terminalId, String trxId, double amount,
                                                long originalSouTransactionId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("terminalId", terminalId);
        request.put("retailId", 1);
        request.put("trxId", trxId);
        request.put("shiftNo", 1);
        request.put("shiftDate", LocalDate.now().toString());
        request.put("receiptNumber", 10);
        request.put("money", money(amount, 0));
        request.put("originalSouTransactionId", originalSouTransactionId);
        request.put("qrPayClientId", 7L);
        request.put("timeZoneId", "Europe/Minsk");
        request.put("operatorId", "op-1");
        request.put("deviceNo", "device-1");
        request.put("terminalIp", "10.0.0.1");
        request.put("receipts", List.of(receipt("Item", amount)));
        return request;
    }

    protected Map<String, Object> confirmRequest(long invoiceId, long souId, long qrPayClientId,
                                                 String trxId, boolean success) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("appId", "app-1");
        request.put("invoiceId", invoiceId);
        request.put("souId", souId);
        request.put("qrPayClientId", qrPayClientId);
        request.put("trxId", trxId);
        request.put("success", success);
        return request;
    }

    protected Map<?, ?> data(Map response, String key) {
        return (Map<?, ?>) response.get(key);
    }

    protected String stateOf(String terminalId, String trnId) {
        return jdbc.queryForObject(
                "select state from public.payment where cash_desk_uid = ? and trn_id = ?",
                String.class, terminalId, trnId);
    }

    protected Boolean successOf(String terminalId, String trnId) {
        return jdbc.queryForObject(
                "select success from public.payment where cash_desk_uid = ? and trn_id = ?",
                Boolean.class, terminalId, trnId);
    }
}
