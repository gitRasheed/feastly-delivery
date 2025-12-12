CREATE TABLE discount_codes (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    percent_bps INT,
    fixed_cents INT,
    scope VARCHAR(30) NOT NULL DEFAULT 'ORDER_ITEMS_ONLY',
    min_items_subtotal_cents INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX idx_discount_codes_code_lower ON discount_codes (LOWER(code));
