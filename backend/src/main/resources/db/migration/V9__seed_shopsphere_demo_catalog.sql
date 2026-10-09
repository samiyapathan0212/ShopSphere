-- ShopSphere F4.2: align the backend catalogue with the 14 canonical frontend
-- demo products so the real cart API can resolve product.id 1-14.
--
-- frontend/src/mocks/catalog.ts is the source of truth for ids, names, prices
-- and category membership. Imagery stays frontend-side: products has no image
-- column, and the cart response returns backend name/price only.
--
-- Two legacy development rows (AERO-PRO, PULSE-3) occupied ids 2 and 3 with
-- stale prices and the wrong category. They are realigned to the canonical
-- catalogue as approved. No cart_items, wishlist_items, reviews, inventories
-- or order_items referenced them, so nothing is orphaned. Any *other* conflict
-- aborts this migration instead of overwriting unrelated data: the product
-- insert below is a plain INSERT, so any residual id/SKU clash raises an error
-- and Flyway rolls the whole migration back.

-- ---------------------------------------------------------------------------
-- Categories: preserve the existing Audio row, add the five missing ones.
-- ON CONFLICT DO NOTHING keeps this non-destructive; the product insert then
-- resolves every category by name, so it is correct regardless of category ids.
-- ---------------------------------------------------------------------------
INSERT INTO categories (id, name, description, active, created_at, updated_at)
VALUES
    (2, 'Wearables', 'Demo wearables category', TRUE, NOW(), NOW()),
    (3, 'Home',      'Demo home and kitchen category', TRUE, NOW(), NOW()),
    (4, 'Workspace', 'Demo workspace category', TRUE, NOW(), NOW()),
    (5, 'Fitness',   'Demo fitness category', TRUE, NOW(), NOW()),
    (6, 'Outdoors',  'Demo outdoors category', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Clear the two authorised legacy rows so the canonical ids/SKUs are free.
-- ---------------------------------------------------------------------------
DELETE FROM products WHERE sku IN ('AERO-PRO', 'PULSE-3');

-- ---------------------------------------------------------------------------
-- The canonical ShopSphere demo catalogue, ids 1-14.
-- ---------------------------------------------------------------------------
INSERT INTO categories (name, description, active, created_at, updated_at)
VALUES ('Audio', 'Audio products category', TRUE, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO products (id, sku, name, description, price, active, category_id, created_at, updated_at)
VALUES
    (1,  'aerowave-pro-headphones',    'AeroWave Pro Wireless Headphones',
     'Over-ear wireless headphones from the ShopSphere demo catalogue.',
     189.00, TRUE, (SELECT id FROM categories WHERE name = 'Audio'),     NOW(), NOW()),
    (2,  'pulsefit-tracker-3',          'PulseFit Tracker 3',
     'Fitness tracker smartwatch from the ShopSphere demo catalogue.',
     119.50, TRUE, (SELECT id FROM categories WHERE name = 'Wearables'), NOW(), NOW()),
    (3,  'emberstone-skillet-12',       'EmberStone Cast Skillet, 12 inch',
     'Cast iron skillet from the ShopSphere demo catalogue.',
      74.00, TRUE, (SELECT id FROM categories WHERE name = 'Home'),      NOW(), NOW()),
    (4,  'meridian-standing-desk',      'Meridian Electric Standing Desk',
     'Electric standing desk from the ShopSphere demo catalogue.',
     429.00, TRUE, (SELECT id FROM categories WHERE name = 'Workspace'), NOW(), NOW()),
    (5,  'trailmark-hiking-boots',      'Trailmark Waterproof Hiking Boots',
     'Waterproof hiking boots from the ShopSphere demo catalogue.',
     165.00, TRUE, (SELECT id FROM categories WHERE name = 'Outdoors'),  NOW(), NOW()),
    (6,  'lumen-desk-lamp',             'Lumen Adjustable Desk Lamp',
     'Adjustable desk lamp from the ShopSphere demo catalogue.',
      84.00, TRUE, (SELECT id FROM categories WHERE name = 'Workspace'), NOW(), NOW()),
    (7,  'corelift-adjustable-dumbbells','CoreLift Adjustable Dumbbell Pair',
     'Adjustable dumbbell pair from the ShopSphere demo catalogue.',
     249.00, TRUE, (SELECT id FROM categories WHERE name = 'Fitness'),   NOW(), NOW()),
    (8,  'brewhaus-precision-kettle',   'BrewHaus Precision Kettle',
     'Precision kettle from the ShopSphere demo catalogue.',
      96.00, TRUE, (SELECT id FROM categories WHERE name = 'Home'),      NOW(), NOW()),
    (9,  'soundbar-lite-2',             'SoundBar Lite 2 Sound System',
     'Compact sound system from the ShopSphere demo catalogue.',
     139.00, TRUE, (SELECT id FROM categories WHERE name = 'Audio'),     NOW(), NOW()),
    (10, 'vertex-yoga-mat',             'Vertex Grip Yoga Mat',
     'Grip yoga mat from the ShopSphere demo catalogue.',
      58.00, TRUE, (SELECT id FROM categories WHERE name = 'Fitness'),   NOW(), NOW()),
    (11, 'atlas-weekender-bag',         'Atlas 40L Weekender Bag',
     '40L weekender bag from the ShopSphere demo catalogue.',
     148.00, TRUE, (SELECT id FROM categories WHERE name = 'Outdoors'),  NOW(), NOW()),
    (12, 'clarity-4k-monitor-27',       'Clarity 27-inch 4K Monitor',
     '27-inch 4K monitor from the ShopSphere demo catalogue.',
     379.00, TRUE, (SELECT id FROM categories WHERE name = 'Workspace'), NOW(), NOW()),
    (13, 'orbit-smart-speaker',         'Orbit Smart Speaker',
     'Smart speaker from the ShopSphere demo catalogue.',
      89.00, TRUE, (SELECT id FROM categories WHERE name = 'Audio'),     NOW(), NOW()),
    (14, 'harvest-air-fryer-xl',        'Harvest XL Air Fryer',
     'Countertop air fryer from the ShopSphere demo catalogue.',
     129.00, TRUE, (SELECT id FROM categories WHERE name = 'Home'),      NOW(), NOW());

-- ---------------------------------------------------------------------------
-- Keep both identity sequences ahead of the explicit ids we just inserted, so
-- future generated inserts do not collide with the seeded catalogue.
-- ---------------------------------------------------------------------------
SELECT setval('products_id_seq',   (SELECT MAX(id) FROM products),   TRUE);
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories), TRUE);
