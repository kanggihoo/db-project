-- 1. Categories
INSERT INTO category (id, name, depth) VALUES (200, 'Electronics SQL', 0);
INSERT INTO category (id, name, depth) VALUES (201, 'Lifestyle SQL', 0);

-- 2. ON_SALE products in category 200
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (200, 200, 'On Sale Product 0', 10000, 'ON_SALE', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (201, 200, 'On Sale Product 1', 10000, 'ON_SALE', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (202, 200, 'On Sale Product 2', 10000, 'ON_SALE', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (203, 200, 'On Sale Product 3', 10000, 'ON_SALE', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (204, 200, 'On Sale Product 4', 10000, 'ON_SALE', false, NOW(), NOW());

-- 3. SOLD_OUT products in category 200
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (205, 200, 'Sold Out Product 0', 10000, 'SOLD_OUT', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (206, 200, 'Sold Out Product 1', 10000, 'SOLD_OUT', false, NOW(), NOW());

-- 4. ON_SALE product in another category
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (207, 201, 'Other Category Product 0', 20000, 'ON_SALE', false, NOW(), NOW());

-- 5. Deleted product; Phase 5 intentionally does not add is_deleted filtering.
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (208, 200, 'Deleted Product 0', 30000, 'ON_SALE', true, NOW(), NOW());

-- 6. SOLD_OUT product in another category to prove null status is not defaulted.
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (209, 201, 'Other Category Sold Out Product 0', 25000, 'SOLD_OUT', false, NOW(), NOW());
