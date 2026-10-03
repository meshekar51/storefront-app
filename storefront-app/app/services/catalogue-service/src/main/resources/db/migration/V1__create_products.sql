CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(32)    NOT NULL UNIQUE,
    name        VARCHAR(120)   NOT NULL,
    description VARCHAR(500),
    category    VARCHAR(40)    NOT NULL,
    price       NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    currency    CHAR(3)        NOT NULL DEFAULT 'EUR',
    active      BOOLEAN        NOT NULL DEFAULT TRUE
);
