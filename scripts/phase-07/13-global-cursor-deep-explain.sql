\if :{?global_deep_offset}
\else
  \set global_deep_offset 100000
\endif

\echo 'PHASE7_GLOBAL_CURSOR_SOURCE'
SELECT created_at AS global_last_created_at,
       id AS global_last_id
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 1 OFFSET :global_deep_offset;

\echo 'PHASE7_GLOBAL_CURSOR_DEEP'
EXPLAIN (ANALYZE, BUFFERS)
WITH cursor_row AS (
    SELECT created_at, id
    FROM point_history
    ORDER BY created_at DESC, id DESC
    LIMIT 1 OFFSET :global_deep_offset
)
SELECT ph.*
FROM point_history ph
CROSS JOIN cursor_row c
WHERE (ph.created_at, ph.id) < (c.created_at, c.id)
ORDER BY ph.created_at DESC, ph.id DESC
LIMIT 20;
