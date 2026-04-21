-- 테스트용 기초 데이터 설정 (주문 N+1 테스트용)

-- 1. 사용자
INSERT INTO users (id, email, password, name, gender, grade, point_balance, created_at, updated_at)
VALUES (100, 'sql-test@test.com', 'pw', '홍길동-SQL', 'MALE', 'BRONZE', 0, NOW(), NOW());

-- 2. 배송지
INSERT INTO user_address (id, user_id, address, detail_address, is_default, receiver_name, receiver_phone)
VALUES (100, 100, '서울시 강남구', '101호-SQL', true, '홍길동-SQL', '010-0000-0000');

-- 3. 카테고리
INSERT INTO category (id, name, depth)
VALUES (100, '테스트카테고리-SQL', 0);

-- 4. 상품
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (100, 100, '테스트상품-SQL', 10000, 'ON_SALE', false, NOW(), NOW());

-- 5. SKU (재고)
INSERT INTO product_sku (id, product_id, sku_code, stock_quantity, extra_price)
VALUES (100, 100, 'SKU-SQL-001', 100, 0);

-- 6. 주문 (3건)
INSERT INTO orders (id, user_id, address_id, total_price, discount_price, final_price, status, created_at)
VALUES (100, 100, 100, 10000, 0, 10000, 'PENDING', NOW());
INSERT INTO orders (id, user_id, address_id, total_price, discount_price, final_price, status, created_at)
VALUES (101, 100, 100, 10000, 0, 10000, 'PENDING', NOW());
INSERT INTO orders (id, user_id, address_id, total_price, discount_price, final_price, status, created_at)
VALUES (102, 100, 100, 10000, 0, 10000, 'PENDING', NOW());

-- 7. 주문 상품
INSERT INTO order_item (id, order_id, sku_id, product_name, quantity, unit_price, status)
VALUES (100, 100, 100, '테스트상품-SQL', 1, 10000, 'PENDING');
INSERT INTO order_item (id, order_id, sku_id, product_name, quantity, unit_price, status)
VALUES (101, 101, 100, '테스트상품-SQL', 1, 10000, 'PENDING');
INSERT INTO order_item (id, order_id, sku_id, product_name, quantity, unit_price, status)
VALUES (102, 102, 100, '테스트상품-SQL', 1, 10000, 'PENDING');
