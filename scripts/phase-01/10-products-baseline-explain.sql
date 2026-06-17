\pset pager off

\echo 'PRODUCTS_BASELINE_EXPLAIN'
EXPLAIN
SELECT *
FROM product
WHERE category_id = 1
  AND status = 'ON_SALE';

\echo 'PRODUCTS_BASELINE_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM product
WHERE category_id = 1
  AND status = 'ON_SALE';
