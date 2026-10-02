-- ShopSphere F5.1A: give the 14 canonical demo products usable inventory.
--
-- V6 created the `inventories` table and seeded every product that existed at
-- that time. V9 then inserted the canonical demo catalogue with explicit ids
-- 1-14, AFTER V6 had already run -- so those products never received the V6
-- stock. `InventoryService.createForProduct` materialises a missing row lazily
-- with `quantity = 0`, which is how rows for ids 1-14 exist today at zero.
--
-- A zero-quantity row is not a harmless placeholder: `Inventory.reserve`
-- rejects it with InsufficientStockException, so CheckoutService answers every
-- customer purchase with 409 Insufficient Stock and the purchase flow is
-- unusable until an admin restocks it by hand.
--
-- This migration tops those rows up. It is deliberately additive and
-- non-destructive:
--
--   * Missing row  -> inserted at 25 units, reserved 0, threshold 10.
--   * Existing row -> quantity raised to GREATEST(quantity, 25, reserved_qty).
--                     A row already holding real stock is therefore never
--                     lowered, and the reserved_qty term guarantees the
--                     quantity can never drop below what is already reserved
--                     (the entity's own guard, Inventory.update, refuses a
--                     quantity below reservedQty; this honours the same rule).
--
-- ON CONFLICT targets uq_inventories_product_id, the existing UNIQUE
-- constraint on product_id, so exactly one row per product is ever produced --
-- no duplicates, on a fresh database or an already-seeded one. Re-running the
-- migration is a no-op, so the result is deterministic either way.
--
-- Scope is the canonical demo ids 1-14 introduced by V9, selected from the
-- products table so the foreign key is always satisfied. No product, price,
-- category or catalogue column is touched.

INSERT INTO inventories (product_id, quantity, reserved_qty, reorder_threshold, created_at, updated_at)
SELECT p.id,
       25,
       0,
       10,
       NOW(),
       NOW()
FROM products p
WHERE p.id BETWEEN 1 AND 14
ON CONFLICT (product_id) DO UPDATE
    SET quantity    = GREATEST(inventories.quantity, EXCLUDED.quantity, inventories.reserved_qty),
        updated_at  = NOW();
