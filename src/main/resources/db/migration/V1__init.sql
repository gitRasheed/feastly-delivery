create table users (
                       id uuid primary key,
                       email varchar(255) unique not null,
                       password varchar(255) not null
);

create table restaurants (
                             id uuid primary key,
                             name varchar(255) not null,
                             address varchar(255) not null,
                             cuisine varchar(100) not null
);

create type order_status as enum (
  'AWAITING_RESTAURANT',
  'PENDING',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
  'CANCELLED'
);

create table orders (
                        id uuid primary key,
                        user_id uuid not null references users(id),
                        restaurant_id uuid not null references restaurants(id),
                        driver_id uuid,
                        status order_status not null default 'AWAITING_RESTAURANT',
                        created_at timestamp not null default now(),
                        updated_at timestamp not null default now()
);
