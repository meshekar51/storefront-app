CREATE TABLE cart_items (
    id           BIGSERIAL PRIMARY KEY,
    cart_id      UUID           NOT NULL,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(120)   NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
