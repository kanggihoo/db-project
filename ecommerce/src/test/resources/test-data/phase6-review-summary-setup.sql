INSERT INTO users (id, email, password, name, gender, grade, point_balance, created_at, updated_at)
VALUES
  (6100, 'phase6-user-0@example.com', 'pw', 'Phase6 User 0', 'MALE', 'GOLD', 0, NOW(), NOW()),
  (6101, 'phase6-user-1@example.com', 'pw', 'Phase6 User 1', 'FEMALE', 'SILVER', 0, NOW(), NOW());

INSERT INTO category (id, name, depth)
VALUES (6200, 'Phase6 Category', 0);

INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES
  (6300, 6200, 'Phase6 Product A', 10000, 'ON_SALE', false, NOW(), NOW()),
  (6301, 6200, 'Phase6 Product B', 12000, 'ON_SALE', false, NOW(), NOW()),
  (6302, 6200, 'Phase6 Product C', 15000, 'ON_SALE', false, NOW(), NOW());

INSERT INTO product_sku (id, product_id, sku_code, stock_quantity, extra_price)
VALUES
  (6400, 6300, 'PHASE6-A', 100, 0),
  (6401, 6301, 'PHASE6-B', 100, 0),
  (6402, 6302, 'PHASE6-C', 100, 0);

INSERT INTO user_address (id, user_id, address, is_default, receiver_name, receiver_phone)
VALUES
  (6500, 6100, 'Phase6 Address 0', true, 'Phase6 User 0', '010-0000-0000'),
  (6501, 6101, 'Phase6 Address 1', true, 'Phase6 User 1', '010-0000-0001');

INSERT INTO orders (id, user_id, address_id, total_price, discount_price, final_price, status, created_at)
VALUES
  (6600, 6100, 6500, 10000, 0, 10000, 'DELIVERED', NOW()),
  (6601, 6101, 6501, 12000, 0, 12000, 'DELIVERED', NOW()),
  (6602, 6100, 6500, 15000, 0, 15000, 'DELIVERED', NOW());

INSERT INTO order_item (id, order_id, sku_id, product_name, quantity, unit_price, status)
VALUES
  (6700, 6600, 6400, 'Phase6 Product A', 1, 10000, 'DELIVERED'),
  (6701, 6601, 6401, 'Phase6 Product B', 1, 12000, 'DELIVERED'),
  (6702, 6602, 6402, 'Phase6 Product C', 1, 15000, 'DELIVERED');

INSERT INTO review (id, user_id, product_id, order_item_id, rating, content, created_at)
SELECT 6800 + n, 6100, 6300, 6700, 5, 'A review ' || n, NOW()
FROM generate_series(0, 9) AS n;

INSERT INTO review (id, user_id, product_id, order_item_id, rating, content, created_at)
SELECT 6810 + n, 6101, 6301, 6701, 4, 'B review ' || n, NOW()
FROM generate_series(0, 9) AS n;

INSERT INTO review (id, user_id, product_id, order_item_id, rating, content, created_at)
SELECT 6820 + n, 6100, 6302, 6702, 3, 'C review ' || n, NOW()
FROM generate_series(0, 8) AS n;
