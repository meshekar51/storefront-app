CREATE TABLE stock (
    product_id BIGINT PRIMARY KEY,
    available  INT NOT NULL CHECK (available >= 0)
);

CREATE TABLE reservations (
    id         BIGSERIAL PRIMARY KEY,
    order_ref  VARCHAR(40) NOT NULL,
    product_id BIGINT      NOT NULL,
    quantity   INT         NOT NULL CHECK (quantity > 0),
    status     VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reservations_order ON reservations (order_ref);

-- Matches catalogue product IDs 1..8. Product 8 starts low to demo "out of stock".
INSERT INTO stock (product_id, available) VALUES
  (1, 25), (2, 40), (3, 15), (4, 60), (5, 20), (6, 80), (7, 30), (8, 3);
