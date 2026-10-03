CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    order_ref     VARCHAR(40)    NOT NULL UNIQUE,
    cart_id       UUID           NOT NULL,
    status        VARCHAR(16)    NOT NULL,
    status_reason VARCHAR(200),
    total         NUMERIC(12, 2) NOT NULL,
    currency      CHAR(3)        NOT NULL,
    payment_ref   VARCHAR(64),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(120)   NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
