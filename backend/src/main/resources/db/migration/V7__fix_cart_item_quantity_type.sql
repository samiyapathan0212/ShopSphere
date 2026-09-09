-- Fix cart_items.quantity type to match the Java Integer field.
ALTER TABLE cart_items
    ALTER COLUMN quantity TYPE INTEGER
    USING quantity::INTEGER;