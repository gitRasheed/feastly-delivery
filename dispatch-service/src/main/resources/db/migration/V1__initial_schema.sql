CREATE TABLE dispatch_attempts (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL,
  driver_id UUID NOT NULL,
  status TEXT NOT NULL,
  offered_at TIMESTAMP NOT NULL,
  responded_at TIMESTAMP
);

