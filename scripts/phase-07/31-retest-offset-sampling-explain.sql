\if :{?user_id}
\else
  \set user_id 707000
\endif

\echo 'PHASE7_RETEST_OFFSET_PAGE0'
\echo 'user_id=' :user_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

\echo 'PHASE7_RETEST_OFFSET_PAGE1000'
\echo 'user_id=' :user_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 20000;

\echo 'PHASE7_RETEST_OFFSET_PAGE4999'
\echo 'user_id=' :user_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 99980;
