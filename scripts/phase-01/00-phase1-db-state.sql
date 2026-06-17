\pset pager off

\echo 'PHASE_01_DB_STATE_CAPTURE'
SELECT CURRENT_TIMESTAMP AS captured_at,
       current_database() AS database_name,
       current_user AS database_user;

\echo 'PHASE_01_TARGET_ROW_COUNTS'
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL SELECT 'product_sku', COUNT(*) FROM product_sku
UNION ALL SELECT 'product_image', COUNT(*) FROM product_image
ORDER BY table_name;

\echo 'PHASE_01_TARGET_INDEXES'
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN (
    'users',
    'product',
    'orders',
    'order_item',
    'point_history',
    'product_sku',
    'product_image'
  )
ORDER BY tablename, indexname;

\echo 'PHASE_01_TARGET_CONSTRAINTS'
SELECT conrelid::regclass AS table_name,
       conname AS constraint_name,
       contype AS constraint_type,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conrelid::regclass::text IN (
    'users',
    'product',
    'orders',
    'order_item',
    'point_history',
    'product_sku',
    'product_image'
  )
ORDER BY conrelid::regclass::text, contype, conname;

\echo 'PHASE_01_PRODUCT_CATEGORY_STATUS_DISTRIBUTION'
SELECT category_id, status, COUNT(*) AS product_count
FROM product
GROUP BY category_id, status
ORDER BY category_id, status;

\echo 'PHASE_01_ORDER_USER_COUNT_SUMMARY'
WITH user_order_counts AS (
  SELECT user_id, COUNT(*) AS order_count
  FROM orders
  GROUP BY user_id
)
SELECT COUNT(*) AS users_with_orders,
       MIN(order_count) AS min_orders_per_user,
       MAX(order_count) AS max_orders_per_user,
       ROUND(AVG(order_count), 2) AS avg_orders_per_user,
       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY order_count) AS median_orders_per_user
FROM user_order_counts;

\echo 'PHASE_01_TOP_ORDER_USERS'
SELECT user_id, COUNT(*) AS order_count
FROM orders
GROUP BY user_id
ORDER BY order_count DESC, user_id
LIMIT 20;

\echo 'PHASE_01_POINT_HISTORY_USER_COUNT_SUMMARY'
WITH user_point_counts AS (
  SELECT user_id, COUNT(*) AS point_count
  FROM point_history
  GROUP BY user_id
)
SELECT COUNT(*) AS users_with_points,
       MIN(point_count) AS min_points_per_user,
       MAX(point_count) AS max_points_per_user,
       ROUND(AVG(point_count), 2) AS avg_points_per_user,
       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY point_count) AS median_points_per_user
FROM user_point_counts;

\echo 'PHASE_01_TOP_POINT_USERS'
SELECT user_id, COUNT(*) AS point_count
FROM point_history
GROUP BY user_id
ORDER BY point_count DESC, user_id
LIMIT 20;
