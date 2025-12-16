CREATE TABLE dispatch_attempts (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,  -- No FK: orders table is in separate database
    driver_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    offered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_dispatch_attempts_order_driver UNIQUE (order_id, driver_id)
);

CREATE INDEX idx_dispatch_attempts_order ON dispatch_attempts(order_id);
CREATE INDEX idx_dispatch_attempts_status ON dispatch_attempts(status) WHERE status = 'PENDING';

