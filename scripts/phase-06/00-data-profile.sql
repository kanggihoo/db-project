\echo 'PHASE6_ROW_COUNTS'
SELECT 'product' AS table_name, COUNT(*) AS row_count FROM product
UNION ALL SELECT 'review', COUNT(*) FROM review
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'users', COUNT(*) FROM users
ORDER BY table_name;

\echo 'PHASE6_REVIEWED_PRODUCT_COUNT'
SELECT COUNT(DISTINCT product_id) AS reviewed_product_count
FROM review;

\echo 'PHASE6_REVIEW_COUNT_DISTRIBUTION'
WITH product_review_counts AS (
  SELECT product_id, COUNT(*) AS review_count
  FROM review
  GROUP BY product_id
)
SELECT MIN(review_count) AS min_review_count,
       MAX(review_count) AS max_review_count,
       ROUND(AVG(review_count), 2) AS avg_review_count
FROM product_review_counts;

\echo 'PHASE6_REVIEW_TOP_20_PRODUCTS'
SELECT product_id, COUNT(*) AS review_count
FROM review
GROUP BY product_id
ORDER BY review_count DESC, product_id ASC
LIMIT 20;

\echo 'PHASE6_MONTHLY_ORDER_DISTRIBUTION'
SELECT DATE_TRUNC('month', created_at) AS order_month,
       COUNT(*) AS order_count,
       SUM(final_price) AS total_revenue
FROM orders
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY order_month DESC;

\echo 'PHASE6_INDEX_STATE_BEFORE'
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('product', 'review', 'orders', 'users')
ORDER BY tablename, indexname;
