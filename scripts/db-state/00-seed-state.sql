\pset pager off

\echo 'SEED_STATE_CAPTURE'
SELECT CURRENT_TIMESTAMP AS captured_at,
       current_database() AS database_name,
       current_user AS database_user;

\echo 'SEED_STATE_ROW_COUNTS'
SELECT 'cart' AS table_name, COUNT(*) AS row_count FROM cart
UNION ALL SELECT 'cart_item', COUNT(*) FROM cart_item
UNION ALL SELECT 'category', COUNT(*) FROM category
UNION ALL SELECT 'coupon', COUNT(*) FROM coupon
UNION ALL SELECT 'delivery', COUNT(*) FROM delivery
UNION ALL SELECT 'delivery_tracking', COUNT(*) FROM delivery_tracking
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'payment', COUNT(*) FROM payment
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'product_image', COUNT(*) FROM product_image
UNION ALL SELECT 'product_option', COUNT(*) FROM product_option
UNION ALL SELECT 'product_option_value', COUNT(*) FROM product_option_value
UNION ALL SELECT 'product_sku', COUNT(*) FROM product_sku
UNION ALL SELECT 'product_sku_option', COUNT(*) FROM product_sku_option
UNION ALL SELECT 'refund', COUNT(*) FROM refund
UNION ALL SELECT 'review', COUNT(*) FROM review
UNION ALL SELECT 'review_image', COUNT(*) FROM review_image
UNION ALL SELECT 'review_like', COUNT(*) FROM review_like
UNION ALL SELECT 'user_address', COUNT(*) FROM user_address
UNION ALL SELECT 'user_coupon', COUNT(*) FROM user_coupon
UNION ALL SELECT 'users', COUNT(*) FROM users
ORDER BY table_name;

\echo 'SEED_STATE_INDEXES'
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

\echo 'SEED_STATE_CONSTRAINTS'
SELECT conrelid::regclass AS table_name,
       conname AS constraint_name,
       contype AS constraint_type,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
ORDER BY conrelid::regclass::text, contype, conname;

\echo 'SEED_STATE_PRODUCT_STATUS_CATEGORY_DISTRIBUTION'
SELECT category_id, status, COUNT(*) AS product_count
FROM product
GROUP BY category_id, status
ORDER BY category_id, status;

\echo 'SEED_STATE_ORDER_STATUS_DISTRIBUTION'
SELECT status, COUNT(*) AS order_count
FROM orders
GROUP BY status
ORDER BY status;

\echo 'SEED_STATE_ORDER_USER_COUNT_SUMMARY'
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

\echo 'SEED_STATE_MONTHLY_ORDER_DISTRIBUTION'
SELECT DATE_TRUNC('month', created_at)::date AS order_month,
       COUNT(*) AS order_count,
       SUM(final_price) AS total_revenue
FROM orders
GROUP BY DATE_TRUNC('month', created_at)::date
ORDER BY order_month;

\echo 'SEED_STATE_POINT_HISTORY_TYPE_DISTRIBUTION'
SELECT type, COUNT(*) AS point_history_count
FROM point_history
GROUP BY type
ORDER BY type;

\echo 'SEED_STATE_POINT_HISTORY_USER_COUNT_SUMMARY'
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

\echo 'SEED_STATE_REVIEW_RATING_DISTRIBUTION'
SELECT rating, COUNT(*) AS review_count
FROM review
GROUP BY rating
ORDER BY rating;

\echo 'SEED_STATE_REVIEW_PRODUCT_COUNT_SUMMARY'
WITH product_review_counts AS (
  SELECT product_id, COUNT(*) AS review_count
  FROM review
  GROUP BY product_id
)
SELECT COUNT(*) AS products_with_reviews,
       MIN(review_count) AS min_reviews_per_product,
       MAX(review_count) AS max_reviews_per_product,
       ROUND(AVG(review_count), 2) AS avg_reviews_per_product,
       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY review_count) AS median_reviews_per_product
FROM product_review_counts;
