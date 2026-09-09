-- Fix reviews.rating type to match the Java Integer field.
ALTER TABLE reviews
    ALTER COLUMN rating TYPE INTEGER
    USING rating::INTEGER;
