\if :{?user_id}
\else
  \set user_id 1
\endif

\echo 'PHASE7_USER_OFFSET_PAGE0'
\echo 'user_id=' :user_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;
