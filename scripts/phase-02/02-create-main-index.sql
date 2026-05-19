CREATE INDEX IF NOT EXISTS idx_product_category_status
  ON product(category_id, status);

ANALYZE product;
