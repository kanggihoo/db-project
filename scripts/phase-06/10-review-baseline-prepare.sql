DROP INDEX IF EXISTS idx_review_product_id;
DROP INDEX IF EXISTS idx_review_product_rating;

VACUUM (ANALYZE) product;
VACUUM (ANALYZE) review;

SELECT pg_stat_statements_reset();
