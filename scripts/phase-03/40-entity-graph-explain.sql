\echo 'Phase 3 entity-graph representative query'
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.id, o.user_id, oi.id AS order_item_id, sku.id AS sku_id, p.id AS product_id
FROM orders o
LEFT JOIN order_item oi ON oi.order_id = o.id
LEFT JOIN product_sku sku ON sku.id = oi.sku_id
LEFT JOIN product p ON p.id = sku.product_id
WHERE o.user_id = 100;
