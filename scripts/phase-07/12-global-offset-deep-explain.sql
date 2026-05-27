\if :{?global_deep_offset}
\else
  \set global_deep_offset 100000
\endif

\echo 'PHASE7_GLOBAL_OFFSET_DEEP'
\echo 'global_deep_offset=' :global_deep_offset
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET :global_deep_offset;
