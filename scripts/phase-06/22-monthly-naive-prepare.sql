DROP INDEX IF EXISTS idx_orders_created_at;
DROP INDEX IF EXISTS idx_orders_month_user;

CREATE INDEX idx_orders_created_at ON orders(created_at);

VACUUM (ANALYZE) orders;
VACUUM (ANALYZE) users;

SELECT pg_stat_statements_reset();
