-- Payment lifecycle state.
--
-- Until now the state of a payment could only be inferred from success, sou_id and
-- error_text, which cannot distinguish "the request was accepted but SOU has not
-- answered yet" from "SOU declined". The new payment service needs that distinction
-- for crash recovery and idempotent retries.
--
-- LEGACY is the default on purpose: rows inserted by cash-desk-api, mobile-api or
-- smartpay-payment-service do not know about this column yet. The new service treats
-- such rows as already final and never retries them against SOU (a retry could
-- charge the customer twice during the transition period).

ALTER TABLE public.payment
    ADD COLUMN IF NOT EXISTS state varchar(32);

UPDATE public.payment
SET state = CASE
    WHEN success THEN 'COMPLETED'
    WHEN error_text IS NOT NULL AND btrim(error_text) <> '' THEN 'DECLINED'
    ELSE 'LEGACY'
END
WHERE state IS NULL;

ALTER TABLE public.payment
    ALTER COLUMN state SET DEFAULT 'LEGACY';

ALTER TABLE public.payment
    ALTER COLUMN state SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'payment_state_ck'
          AND conrelid = 'public.payment'::regclass
    ) THEN
        ALTER TABLE public.payment
            ADD CONSTRAINT payment_state_ck
            CHECK (state IN ('LEGACY', 'CREATED', 'CONFIRMED', 'COMPLETED', 'DECLINED', 'CANCELLED', 'STORNO_PENDING'));
    END IF;
END
$$;
