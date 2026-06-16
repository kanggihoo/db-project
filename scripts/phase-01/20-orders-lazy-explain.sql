\pset pager off
\set order_user_id 950

\echo 'ORDERS_BY_USER_EXPLAIN'
EXPLAIN
SELECT *
FROM orders
WHERE user_id = :order_user_id
ORDER BY id ASC;

\echo 'ORDERS_BY_USER_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM orders
WHERE user_id = :order_user_id
ORDER BY id ASC;

\echo 'ORDER_ITEM_BY_ORDER_ID_EXPLAIN'
EXPLAIN
SELECT *
FROM order_item
WHERE order_id = (
  SELECT id
  FROM orders
  WHERE user_id = :order_user_id
  ORDER BY id ASC
  LIMIT 1
);

\echo 'ORDER_ITEM_BY_ORDER_ID_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM order_item
WHERE order_id = (
  SELECT id
  FROM orders
  WHERE user_id = :order_user_id
  ORDER BY id ASC
  LIMIT 1
);
