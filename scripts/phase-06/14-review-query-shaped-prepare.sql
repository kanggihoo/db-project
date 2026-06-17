DROP INDEX IF EXISTS idx_review_product_id;
DROP INDEX IF EXISTS idx_review_product_rating;

CREATE INDEX idx_review_product_rating ON review(product_id, rating);

VACUUM (ANALYZE) product;
VACUUM (ANALYZE) review;

SELECT pg_stat_statements_reset();
