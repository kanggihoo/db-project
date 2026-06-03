\echo 'PHASE7_POINT_HISTORY_TOTAL_COUNT'
SELECT COUNT(*) AS point_history_count
FROM point_history;

\echo 'PHASE7_HOT_USER_POINT_COUNTS'
SELECT user_id, COUNT(*) AS point_count
FROM point_history
GROUP BY user_id
ORDER BY point_count DESC
LIMIT 20;

\echo 'PHASE7_INDEX_STATE_BEFORE'
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'point_history'
ORDER BY indexname;
