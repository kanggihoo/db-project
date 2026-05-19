DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

ANALYZE product;
