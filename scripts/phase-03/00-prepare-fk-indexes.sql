\echo 'Phase 3 prepare FK lookup indexes'

CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON orders (user_id);

CREATE INDEX IF NOT EXISTS idx_order_item_order_id
    ON order_item (order_id);

CREATE INDEX IF NOT EXISTS idx_product_image_product_id
    ON product_image (product_id);
