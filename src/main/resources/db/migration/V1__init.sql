create table users (
  id uuid primary key,
  email varchar(255) unique not null,
  password varchar(255) not null
);

create table restaurants (
  id uuid primary key,
  name varchar(255) not null
);

create table orders (
  id uuid primary key,
  user_id uuid references users(id),
  restaurant_id uuid references restaurants(id),
  status varchar(50) not null
);

