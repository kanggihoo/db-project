\if :{?user_id}
\else
  \set user_id 1
\endif

\if :{?deep_offset}
\else
  \set deep_offset 1000
\endif

\echo 'PHASE7_USER_OFFSET_DEEP'
\echo 'user_id=' :user_id
\echo 'deep_offset=' :deep_offset
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET :deep_offset;
