CREATE TABLE payments (
    id           BIGSERIAL PRIMARY KEY,
    order_ref    VARCHAR(40)    NOT NULL UNIQUE, -- idempotency key
    amount       NUMERIC(10, 2) NOT NULL,
    currency     CHAR(3)        NOT NULL,
    status       VARCHAR(16)    NOT NULL,
    provider_ref VARCHAR(64),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);
