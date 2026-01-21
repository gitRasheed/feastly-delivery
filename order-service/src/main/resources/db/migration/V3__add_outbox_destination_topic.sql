-- Add destination_topic column to support dynamic Kafka topic routing
-- Default to 'order.events' for backward compatibility with existing outbox entries
ALTER TABLE order_outbox
    ADD COLUMN destination_topic VARCHAR(255) NOT NULL DEFAULT 'order.events';
