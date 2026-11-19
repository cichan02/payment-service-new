# payment-service-new

Payment domain service for SmartPay. It owns payment, storno, refund, invoice
confirmation and payment history, persists `public.payment` / `public.receipt`,
and is the only new service that talks to SOU about payments.

The cutover from `cash-desk-api` / `mobile-api` is described in
[`../docs/payment-service-new-cutover.md`](../docs/payment-service-new-cutover.md).

- Base path: `/api/v1/payment-new`
- Port: `10280`
- Transport: `POST`, `application/json`
- Java 21, Spring Boot 3.5, Spring Cloud OpenFeign, JPA, Flyway (pipeline only)

The word "payment" is used in the API; the database still stores
`payment_type = 'SALE'` for a normal payment because cash-desk reporting and
`payments_view` depend on that value.

---

## Run

Environment:

| Variable | Meaning |
|---|---|
| `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB` | shared SmartPay database |
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | database credentials |
| `SOU_URL` | SOU base URL, e.g. `http://sou-service:8080` (default `http://sou-service`) |

```bash
./mvnw clean package
java -jar target/payment-service-new-0.0.1.jar
```

Health: `GET /api/v1/payment-new/actuator/health`.

The schema migration is **not** applied at startup. Run it once per environment:

```bash
./mvnw flyway:migrate \
  -Dflyway.url=jdbc:postgresql://$POSTGRES_HOST:$POSTGRES_PORT/$POSTGRES_DB \
  -Dflyway.user=$POSTGRES_USER \
  -Dflyway.password=$POSTGRES_PASSWORD
```

The module uses its own history table `flyway_schema_history_payment_service_new`.
The migration adds `public.payment.state` and leaves rows written by the old
services at `LEGACY`, so they are never retried against SOU.

---

## Operations

### `POST /payment`

Creates or resumes a payment. `operationDateTime` is not accepted: the service
generates it. `terminalId + trxId` is the idempotency key.

```json
{
  "terminalId": "QR0001",
  "retailId": 10,
  "trxId": "0f8a1c",
  "shiftNo": 42,
  "shiftDate": "2026-11-19",
  "receiptNumber": 1234,
  "money": {
    "amount": 12.50,
    "discount": 0.00,
    "currency": 933,
    "currencyAbbr": "BYN"
  },
  "qrPayClientId": 98765,
  "souId": 123456789,
  "payToolId": 321,
  "smartPaymentCardNumber": null,
  "operatorId": "op-17",
  "operatorName": "Ivanov I.",
  "deviceNo": "POS-3",
  "timeZoneId": "Europe/Minsk",
  "terminalIp": "10.10.1.20",
  "mobileQueue": null,
  "qrVersion": null,
  "loyaltyJson": null,
  "receipts": [
    {
      "name": "Coffee",
      "price": 12.50,
      "money": {"amount": 12.50, "discount": 0.00, "currency": 933},
      "quantity": 1,
      "unitName": "pcs",
      "barCode": "4810000000001"
    }
  ]
}
```

Response `200`:

```json
{
  "errorCode": 0,
  "errorMessage": "",
  "operationTime": "2026-11-19T11:40:12.123",
  "trxId": "0f8a1c",
  "operationId": 998877,
  "slip": "line 1\nline 2",
  "receiptSlip": ["line 1", "line 2"],
  "payment": {
    "id": 918273,
    "cashDeskUid": "QR0001",
    "trnId": "0f8a1c",
    "paymentDate": "2026-11-19T08:40:11",
    "money": {"amount": 12.50, "discount": 0.00, "currency": 933, "currencyAbbr": "BYN"},
    "success": true,
    "souId": 998877,
    "paymentType": "SALE",
    "shiftNumber": 42,
    "receiptNumber": "1234",
    "qrPayClientId": 98765,
    "storno": false,
    "state": "COMPLETED",
    "terminalIp": "10.10.1.20",
    "errorText": null,
    "payToolId": 321,
    "smartPaymentCardNumber": null
  },
  "receipts": [
    {
      "id": 771,
      "paymentId": 918273,
      "name": "Coffee",
      "price": 12.50,
      "quantity": 1,
      "unitName": "pcs",
      "barCode": "4810000000001",
      "money": {"amount": 12.50, "discount": 0.00, "currency": 933}
    }
  ]
}
```

If SOU is unreachable the service returns `503` with `errorCode -1300` and leaves
the row in `state = CREATED`. **Retry the exact same request.** Do not fall back
to a direct SOU call: that can charge the customer twice.

If the same `trxId` is reused with a different amount, discount or currency the
service returns `409 -4402`.

### `POST /storno`

Cash-desk side cancellation. The sale is identified by `terminalId + trxId`; the
stored row gets the `-S` suffix. If the sale has not been seen yet, the service
stores `STORNO_PENDING`, returns `-4004`, and applies the storno automatically
when the payment arrives (the payment then returns `-9295`).

```json
{
  "terminalId": "QR0001",
  "retailId": 10,
  "trxId": "0f8a1c",
  "shiftNo": 42,
  "shiftDate": "2026-11-19",
  "receiptNumber": 1234,
  "money": {"amount": 12.50, "discount": 0.00, "currency": 933, "currencyAbbr": "BYN"},
  "qrPayClientId": 98765,
  "operatorId": "op-17",
  "deviceNo": "POS-3",
  "timeZoneId": "Europe/Minsk",
  "terminalIp": "10.10.1.20",
  "receipts": []
}
```

The amount always follows the successful sale, even if the request carries a
different value. `receipts` is optional; when empty a single default line is
created.

### `POST /refund`

Customer side cancellation. `originalSouTransactionId` is the SOU transaction
number of the payment being refunded. The refund is a new transaction stored with
the `-R` suffix and inherits the client and pay tool of the original payment.

```json
{
  "terminalId": "QR0001",
  "retailId": 10,
  "trxId": "0f8a1c",
  "shiftNo": 42,
  "shiftDate": "2026-11-19",
  "receiptNumber": 1234,
  "money": {"amount": 12.50, "discount": 0.00, "currency": 933, "currencyAbbr": "BYN"},
  "originalSouTransactionId": 998877,
  "qrPayClientId": 98765,
  "payToolId": 321,
  "timeZoneId": "Europe/Minsk",
  "terminalIp": "10.10.1.20",
  "receipts": []
}
```

`404 -4403` means the original payment is unknown to this database.

### `POST /confirm`

Mobile side invoice confirmation/cancellation. Signature and device checks stay
in `mobile-api`. `success = true` records `CONFIRMED`, `success = false` records
`CANCELLED`. The `payment.success` flag is never changed by this endpoint.

`trxId` is optional because `mobile-api`'s cancel request does not carry one. It
is needed to update the stored payment row.

```json
{
  "appId": "com.besmart.qrpay",
  "invoiceId": 55501,
  "souId": 123456789,
  "qrPayClientId": 98765,
  "trxId": "0f8a1c",
  "payToolId": 321,
  "smartPaymentCardNumber": null,
  "currencyAbbr": "BYN",
  "clientIp": "10.10.2.7",
  "success": true
}
```

Returns `200` when SOU accepts, `400` with the SOU error code otherwise.

### `POST /payment-history`

Returns persisted payment rows with their receipt rows. No `payments_view`, no
store/retail enrichment — `mobile-api` keeps that.

```json
{
  "qrPayClientId": 98765,
  "dateFrom": 1763510400000,
  "dateTo": 1763596799999,
  "payToolId": null,
  "cardNumber": null
}
```

```json
{
  "errorCode": 0,
  "errorMessage": "",
  "operationTime": "2026-11-19T11:45:00",
  "payments": [
    {
      "payment": {"id": 918273, "trnId": "0f8a1c", "souId": 998877, "state": "COMPLETED", "...": "..."},
      "receipts": [{"id": 771, "name": "Coffee", "...": "..."}]
    }
  ]
}
```

Sorted by `paymentDate desc, id desc`. `dateFrom > dateTo` returns `400 -4401`.

---

## States

| State | Meaning | SOU retry |
|---|---|---|
| `LEGACY` | written by an old service, unknown to the state machine | never |
| `CREATED` | intent committed, SOU has not answered | same request only |
| `COMPLETED` | SOU accepted | no |
| `CONFIRMED` | mobile confirmed the invoice | no |
| `DECLINED` | SOU declined | no |
| `CANCELLED` | mobile cancelled the invoice | no |
| `STORNO_PENDING` | storno arrived before its sale | after the sale completes |

## Error codes

| Code | Meaning |
|---|---|
| `0` | success |
| `-1300` | SOU unavailable (HTTP 503, retry the same request) |
| `-4004` | sale transaction not found (storno is now `STORNO_PENDING`) |
| `-4400` | unexpected service error |
| `-4401` | validation error |
| `-4402` | idempotency conflict (same trx, different money) |
| `-4403` | original payment for refund not found |
| `-4404` | stored decline on a repeated request |
| `-9295` | payment cancelled by cash desk (delayed storno) |

---

## Tests

```bash
./mvnw test
```

Requires Docker. The suite starts `postgres:18.4` in Testcontainers, applies the
real Flyway migration on top of a base schema, and runs a JDK `HttpServer` SOU
stub. Scenarios are listed in the cutover plan, section 12.

## Configuration

`src/main/resources/application.yml`:

- `server.servlet.context-path: /api/v1/payment-new`
- `spring.jpa.hibernate.ddl-auto: none` (the schema is owned by migrations)
- `spring.flyway.enabled: false` (migrations run from the pipeline)
- `spring.cloud.openfeign.client.config.sou-service.connect-timeout: 5000`
- `spring.cloud.openfeign.client.config.sou-service.read-timeout: 45000`
- `sou.url` from `SOU_URL`
