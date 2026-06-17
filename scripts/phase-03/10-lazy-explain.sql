\echo 'Phase 3 lazy representative query: order_item by order_id'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, option_info, order_id, product_name, quantity, sku_id, status, unit_price
FROM order_item
WHERE order_id = 100;
