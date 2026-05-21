\echo 'SQL-only: single-column status index selectivity'

DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

ANALYZE product;

\echo 'Before status index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE status = 'ON_SALE';

CREATE INDEX idx_product_status
  ON product(status);

ANALYZE product;

\echo 'After status index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE status = 'ON_SALE';
