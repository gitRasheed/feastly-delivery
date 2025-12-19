-- Add menu_item_name column to store snapshot of menu item name at order creation time.
-- This enables order reads to be independent of restaurant-service availability.
ALTER TABLE order_items ADD COLUMN menu_item_name TEXT;
