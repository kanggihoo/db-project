\echo 'Phase 2 main product query: pre-index'

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';
