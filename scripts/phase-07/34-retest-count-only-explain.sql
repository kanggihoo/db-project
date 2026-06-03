\if :{?user_id}
\else
  \set user_id 707000
\endif

\echo 'PHASE7_RETEST_COUNT_ONLY'
\echo 'user_id=' :user_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM point_history
WHERE user_id = :user_id;
