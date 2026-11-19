-- Base schema for integration tests.
--
-- The payment and receipt tables predate Flyway in this database, so there is no
-- canonical creation script in the repositories. This file mirrors the columns
-- actually used by cash-desk-api, mobile-api and smartpay-payment-service; the real
-- migration V1.0.0__add_payment_state.sql runs on top of it, so the migration itself
-- is exercised by the tests.

CREATE TABLE IF NOT EXISTS public.payment
(
    id                         bigserial PRIMARY KEY,
    cash_desk_uid              varchar(40)  NOT NULL,
    trn_id                     varchar(50)  NOT NULL,
    payment_date               timestamp    NOT NULL,
    amount                     numeric(19, 2) NOT NULL,
    discount                   numeric(19, 2) NOT NULL DEFAULT 0,
    success                    boolean      NOT NULL DEFAULT false,
    sou_id                     bigint,
    payment_type               varchar(10),
    shift_number               integer,
    currency                   integer,
    receipt_number             varchar(50),
    qrpay_client_id            bigint,
    currency_abbr              varchar(3),
    is_storno                  boolean      NOT NULL DEFAULT false,
    terminal_ip                varchar(255),
    qrpay_client_ip            varchar(20),
    error_text                 varchar(2000),
    pay_tool_id                bigint,
    smart_payment_card_number  varchar(20),
    bic                        varchar(30)
);

CREATE TABLE IF NOT EXISTS public.receipt
(
    id         bigserial PRIMARY KEY,
    payment_id bigint       NOT NULL,
    name       varchar(120) NOT NULL,
    price      numeric(19, 2),
    currency   integer,
    quantity   numeric(19, 3),
    unit_name  varchar(20),
    amount     numeric(19, 2),
    discount   numeric(19, 2),
    bar_code   varchar(20)
);

CREATE INDEX IF NOT EXISTS payment_qrpay_client_id_idx ON public.payment (qrpay_client_id);
CREATE INDEX IF NOT EXISTS receipt_payment_id_idx ON public.receipt (payment_id);
