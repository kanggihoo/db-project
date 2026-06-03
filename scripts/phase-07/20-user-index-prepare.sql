DROP INDEX IF EXISTS idx_point_history_user_created_id;

CREATE INDEX idx_point_history_user_created_id
ON point_history (user_id, created_at DESC, id DESC);

VACUUM (ANALYZE) point_history;

SELECT pg_stat_statements_reset();
