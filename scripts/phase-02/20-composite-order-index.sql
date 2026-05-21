\echo 'SQL-only: composite index order'

DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

CREATE INDEX idx_product_category_status_created
  ON product(category_id, status, created_at DESC);

CREATE INDEX idx_product_status_category_created
  ON product(status, category_id, created_at DESC);

ANALYZE product;

\echo 'category_id only with ORDER BY created_at'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
ORDER BY created_at DESC;

\echo 'category_id and status with ORDER BY created_at'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE'
ORDER BY created_at DESC;

\echo 'category_id with created_at range'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND created_at >= TIMESTAMP '2026-01-01 00:00:00';
