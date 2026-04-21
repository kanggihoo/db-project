-- 1. 카테고리 설정
INSERT INTO category (id, name, depth) VALUES (200, '전자제품-SQL', 0);

-- 2. ON_SALE 상품 5건
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (200, 200, '판매중상품0', 10000, 'ON_SALE', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (201, 200, '판매중상품1', 10000, 'ON_SALE', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (202, 200, '판매중상품2', 10000, 'ON_SALE', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (203, 200, '판매중상품3', 10000, 'ON_SALE', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (204, 200, '판매중상품4', 10000, 'ON_SALE', false, NOW(), NOW());

-- 3. SOLD_OUT 상품 2건
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (205, 200, '품절상품0', 10000, 'SOLD_OUT', false, NOW(), NOW());
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (206, 200, '품절상품1', 10000, 'SOLD_OUT', false, NOW(), NOW());
