SELECT pg_stat_statements_reset();
VACUUM ANALYZE orders;
VACUUM ANALYZE order_item;
VACUUM ANALYZE product_sku;
VACUUM ANALYZE product;
VACUUM ANALYZE product_image;
