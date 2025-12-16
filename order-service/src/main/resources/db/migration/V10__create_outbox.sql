-- Outbox table for transactional event publishing
CREATE TABLE order_outbox (
    id UUID PRIMARY KEY,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    processed BOOLEAN DEFAULT false
);

-- Index for efficient polling of unprocessed entries
CREATE INDEX idx_outbox_unprocessed ON order_outbox(processed) WHERE processed = false;
