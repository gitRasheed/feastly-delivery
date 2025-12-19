CREATE TABLE users_customers (
  id UUID PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  phone TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE users_drivers (
  id UUID PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  phone TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE users_restaurants (
  id UUID PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  phone TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now()
);

