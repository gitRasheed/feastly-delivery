-- Remove FK constraints to allow order-service to work independently
-- The users and restaurants tables are now in separate microservices

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_user_id_fkey;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_restaurant_id_fkey;
ALTER TABLE ratings DROP CONSTRAINT IF EXISTS ratings_user_id_fkey;
ALTER TABLE menu_items DROP CONSTRAINT IF EXISTS menu_items_restaurant_id_fkey;

-- Also drop the users and restaurants tables as they're now owned by separate services
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS restaurants CASCADE;
