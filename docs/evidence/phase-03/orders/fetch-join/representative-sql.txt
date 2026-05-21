\echo 'Phase 3 fetch-join representative query'
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.id, o.user_id, oi.id AS order_item_id, sku.id AS sku_id, p.id AS product_id
FROM orders o
JOIN order_item oi ON oi.order_id = o.id
JOIN product_sku sku ON sku.id = oi.sku_id
JOIN product p ON p.id = sku.product_id
WHERE o.user_id = 100;
