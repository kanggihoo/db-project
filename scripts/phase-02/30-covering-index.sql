\echo 'SQL-only: covering index'

DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

ANALYZE product;

\echo 'Before covering index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';

CREATE INDEX idx_product_covering
  ON product(category_id, status)
  INCLUDE (id, name, base_price);

VACUUM ANALYZE product;

\echo 'After covering index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';
