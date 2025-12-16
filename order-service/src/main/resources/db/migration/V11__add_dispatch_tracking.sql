-- Add dispatch tracking columns for retry logic
ALTER TABLE orders ADD COLUMN dispatch_attempt_count INTEGER DEFAULT 0;
ALTER TABLE orders ADD COLUMN dispatch_sent_at TIMESTAMP;
