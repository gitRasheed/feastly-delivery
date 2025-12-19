CREATE TABLE restaurants (
  id UUID PRIMARY KEY,
  owner_user_id UUID NOT NULL,
  name TEXT NOT NULL,
  is_open BOOLEAN NOT NULL,
  opens_at TIME,
  closes_at TIME,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE menu_categories (
  id UUID PRIMARY KEY,
  restaurant_id UUID NOT NULL,
  name TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE menu_items (
  id UUID PRIMARY KEY,
  restaurant_id UUID NOT NULL,
  category_id UUID,
  name TEXT NOT NULL,
  description TEXT,
  price_cents INT NOT NULL,
  available BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

