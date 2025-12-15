CREATE TABLE dispatch_attempt (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    driver_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    offered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_dispatch_attempt_order_driver UNIQUE (order_id, driver_id)
);

CREATE INDEX idx_dispatch_attempt_order ON dispatch_attempt(order_id);
CREATE INDEX idx_dispatch_attempt_status ON dispatch_attempt(status) WHERE status = 'PENDING';
