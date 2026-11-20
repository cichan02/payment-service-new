package com.besmart.qrpay.paymentservicenew;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal SOU double. Replaces WireMock because it is unavailable in the local
 * Maven repository and the contract is small.
 */
public final class SouStub implements AutoCloseable {

    public record Outcome(int httpStatus, int errorCode, String errorMessage, long souTransaction) {

        public static Outcome success(long souTransaction) {
            return new Outcome(200, 0, "", souTransaction);
        }

        public static Outcome decline(int errorCode, String message) {
            return new Outcome(200, errorCode, message, 0);
        }

        public static Outcome httpError() {
            return new Outcome(500, -1, "SOU is down", 0);
        }
    }

    public record Recorded(String path, String body) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    private volatile Outcome payment = Outcome.success(555);
    private volatile Outcome storno = Outcome.success(556);
    private volatile Outcome refund = Outcome.success(557);
    private volatile Outcome confirm = Outcome.success(0);

    private SouStub(HttpServer server) {
        this.server = server;
    }

    public static SouStub start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SouStub stub = new SouStub(server);
            server.createContext("/", stub::handle);
            server.start();
            return stub;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot start SOU stub", ex);
        }
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void reset() {
        payment = Outcome.success(555);
        storno = Outcome.success(556);
        refund = Outcome.success(557);
        confirm = Outcome.success(0);
        requests.clear();
        counters.clear();
    }

    public void payment(Outcome outcome) {
        this.payment = outcome;
    }

    public void storno(Outcome outcome) {
        this.storno = outcome;
    }

    public void refund(Outcome outcome) {
        this.refund = outcome;
    }

    public void confirm(Outcome outcome) {
        this.confirm = outcome;
    }

    public int count(String operation) {
        AtomicInteger value = counters.get(operation);
        return value == null ? 0 : value.get();
    }

    public List<Recorded> requests() {
        return List.copyOf(requests);
    }

    public Recorded last(String operation) {
        return requests.stream()
                .filter(request -> operation.equals(operationOf(request.path())))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String operation = operationOf(path);
        requests.add(new Recorded(path, body));
        counters.computeIfAbsent(operation, key -> new AtomicInteger()).incrementAndGet();

        Outcome outcome = switch (operation) {
            case "payment" -> payment;
            case "storno" -> storno;
            case "refund" -> refund;
            case "invoicePayment" -> confirm;
            default -> new Outcome(404, -1, "unknown operation", 0);
        };

        Map<String, Object> payload = outcome.errorCode() == 0
                ? successPayload(operation, outcome)
                : Map.of(
                        "operationTime", "19.11.2026 10:00:00",
                        "errorCode", outcome.errorCode(),
                        "errorMessage", outcome.errorMessage());

        byte[] response = MAPPER.writeValueAsBytes(payload);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(outcome.httpStatus(), response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private Map<String, Object> successPayload(String operation, Outcome outcome) {
        if ("invoicePayment".equals(operation)) {
            return Map.of(
                    "operationTime", "19.11.2026 10:00:00",
                    "errorCode", 0,
                    "errorMessage", "");
        }
        return Map.of(
                "operationTime", "19.11.2026 10:00:00",
                "errorCode", 0,
                "errorMessage", "",
                "souTransaction", outcome.souTransaction(),
                "slip", "slip line 1\nslip line 2");
    }

    private String operationOf(String path) {
        if (path.endsWith("/invoicePayment")) {
            return "invoicePayment";
        }
        if (path.endsWith("/payment")) {
            return "payment";
        }
        if (path.endsWith("/storno")) {
            return "storno";
        }
        if (path.endsWith("/refund")) {
            return "refund";
        }
        return "unknown";
    }
}
