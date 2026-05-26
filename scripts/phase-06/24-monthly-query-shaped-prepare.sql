DROP INDEX IF EXISTS idx_orders_created_at;
DROP INDEX IF EXISTS idx_orders_month_user;

CREATE INDEX idx_orders_month_user
ON orders ((DATE_TRUNC('month', created_at)), user_id);

VACUUM (ANALYZE) orders;
VACUUM (ANALYZE) users;

SELECT pg_stat_statements_reset();
