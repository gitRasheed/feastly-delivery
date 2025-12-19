CREATE TABLE orders (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL,
  restaurant_id UUID NOT NULL,
  driver_id UUID,
  status TEXT NOT NULL,
  subtotal_cents INT NOT NULL,
  tax_cents INT NOT NULL,
  delivery_fee_cents INT NOT NULL,
  total_cents INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL,
  menu_item_id UUID NOT NULL,
  quantity INT NOT NULL,
  price_cents INT NOT NULL
);

CREATE TABLE order_outbox (
  id UUID PRIMARY KEY,
  aggregate_id UUID NOT NULL,
  aggregate_type TEXT NOT NULL,
  event_type TEXT NOT NULL,
  payload JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  published_at TIMESTAMP
);

CREATE TABLE ratings (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL,
  customer_id UUID NOT NULL,
  rating INT NOT NULL,
  comment TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE discount_codes (
  id UUID PRIMARY KEY,
  code TEXT UNIQUE NOT NULL,
  percentage INT,
  amount_cents INT,
  active BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

