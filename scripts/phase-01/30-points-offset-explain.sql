\pset pager off
\set point_user_id 5714
\set page_size 20
\set page500_offset 10000

\echo 'POINTS_PAGE0_EXPLAIN'
EXPLAIN
SELECT *
FROM point_history
WHERE user_id = :point_user_id
ORDER BY created_at DESC, id DESC
LIMIT :page_size OFFSET 0;

\echo 'POINTS_PAGE0_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :point_user_id
ORDER BY created_at DESC, id DESC
LIMIT :page_size OFFSET 0;

\echo 'POINTS_PAGE500_EXPLAIN'
EXPLAIN
SELECT *
FROM point_history
WHERE user_id = :point_user_id
ORDER BY created_at DESC, id DESC
LIMIT :page_size OFFSET :page500_offset;

\echo 'POINTS_PAGE500_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :point_user_id
ORDER BY created_at DESC, id DESC
LIMIT :page_size OFFSET :page500_offset;

\echo 'POINTS_COUNT_EXPLAIN'
EXPLAIN
SELECT COUNT(*)
FROM point_history
WHERE user_id = :point_user_id;

\echo 'POINTS_COUNT_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*)
FROM point_history
WHERE user_id = :point_user_id;
