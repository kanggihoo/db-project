\echo 'SQL-only: partial index for active products'

DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

ANALYZE product;

\echo 'Before partial index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE'
  AND is_deleted = false;

CREATE INDEX idx_product_active_category_status
  ON product(category_id, status)
  WHERE is_deleted = false;

ANALYZE product;

\echo 'After partial index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE'
  AND is_deleted = false;
