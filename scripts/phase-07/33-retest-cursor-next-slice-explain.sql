\if :{?user_id}
\else
  \set user_id 707000
\endif

\if :{?last_created_at}
\else
  \echo 'last_created_at is required'
  \quit 1
\endif

\if :{?last_id}
\else
  \echo 'last_id is required'
  \quit 1
\endif

\echo 'PHASE7_RETEST_CURSOR_NEXT_SLICE'
\echo 'user_id=' :user_id
\echo 'last_created_at=' :'last_created_at'
\echo 'last_id=' :last_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
  AND (created_at, id) < (:'last_created_at'::timestamp, :last_id)
ORDER BY created_at DESC, id DESC
LIMIT 20;
