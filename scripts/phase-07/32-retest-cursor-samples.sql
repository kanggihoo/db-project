\if :{?user_id}
\else
  \set user_id 707000
\endif

\echo 'PHASE7_RETEST_CURSOR_SAMPLES'
\echo 'user_id=' :user_id
WITH sample_pages(page_no) AS (
    VALUES
        (0),
        (10),
        (50),
        (100),
        (500),
        (1000),
        (2000),
        (3000),
        (4000),
        (4999)
),
sample_offsets AS (
    SELECT page_no,
           page_no * 20 AS target_offset,
           page_no * 20 - 1 AS cursor_source_offset
    FROM sample_pages
)
SELECT s.page_no,
       s.target_offset,
       s.cursor_source_offset,
       ph.created_at AS last_created_at,
       ph.id AS last_id
FROM sample_offsets s
LEFT JOIN LATERAL (
    SELECT created_at, id
    FROM point_history
    WHERE user_id = :user_id
    ORDER BY created_at DESC, id DESC
    LIMIT 1 OFFSET GREATEST(s.cursor_source_offset, 0)
) ph ON s.page_no > 0
ORDER BY s.page_no;
