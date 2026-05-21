\if :{?user_id}
\else
\set user_id 100
\endif

\echo 'Phase 3 selected order-list shape'
SELECT COUNT(DISTINCT o.id) AS order_count,
       COUNT(oi.id) AS order_item_count,
       COUNT(DISTINCT oi.sku_id) AS distinct_sku_count,
       COUNT(DISTINCT ps.product_id) AS distinct_product_count
FROM orders o
JOIN order_item oi ON oi.order_id = o.id
JOIN product_sku ps ON ps.id = oi.sku_id
WHERE o.user_id = :user_id;
