-- Add payment tracking to orders
ALTER TABLE orders ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE orders ADD COLUMN payment_reference VARCHAR(255);

CREATE INDEX idx_orders_payment_status ON orders(payment_status);
