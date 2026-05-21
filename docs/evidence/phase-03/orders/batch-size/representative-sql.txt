\echo 'Phase 3 batch-size representative query: order_item IN order_ids'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, option_info, order_id, product_name, quantity, sku_id, status, unit_price
FROM order_item
WHERE order_id IN (100, 101, 102);
