\if :{?user_id}
\else
  \set user_id 1
\endif

\if :{?deep_offset}
\else
  \set deep_offset 1000
\endif

\echo 'PHASE7_USER_CURSOR_SOURCE'
SELECT created_at AS user_last_created_at,
       id AS user_last_id
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 1 OFFSET :deep_offset;

\echo 'PHASE7_USER_CURSOR_DEEP'
EXPLAIN (ANALYZE, BUFFERS)
WITH cursor_row AS (
    SELECT created_at, id
    FROM point_history
    WHERE user_id = :user_id
    ORDER BY created_at DESC, id DESC
    LIMIT 1 OFFSET :deep_offset
)
SELECT ph.*
FROM point_history ph
CROSS JOIN cursor_row c
WHERE ph.user_id = :user_id
  AND (ph.created_at, ph.id) < (c.created_at, c.id)
ORDER BY ph.created_at DESC, ph.id DESC
LIMIT 20;
