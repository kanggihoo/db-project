# 013 Retest SQL Scripts And Cursor Samples

## Goal

A/B/C 재측정에 필요한 SQL-only evidence 스크립트와 Cursor sample 생성 스크립트를 추가한다.

## Files

- Create: `scripts/phase-07/31-retest-offset-sampling-explain.sql`
- Create: `scripts/phase-07/32-retest-cursor-samples.sql`
- Create: `scripts/phase-07/33-retest-cursor-next-slice-explain.sql`
- Create: `scripts/phase-07/34-retest-count-only-explain.sql`
- Output later: `docs/evidence/phase-07/data-profile/retest-cursor-samples.txt`
- Output later: `docs/evidence/phase-07/data-profile/retest-cursor-samples.json`

## Steps

- [ ] **Step 1: Create Offset sampling EXPLAIN SQL**

Create `scripts/phase-07/31-retest-offset-sampling-explain.sql`.

```sql
\if :{?user_id}
\else
  \set user_id 707000
\endif

\echo 'PHASE7_RETEST_OFFSET_PAGE0'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

\echo 'PHASE7_RETEST_OFFSET_PAGE1000'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 20000;

\echo 'PHASE7_RETEST_OFFSET_PAGE4999'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 99980;
```

- [ ] **Step 2: Create Cursor sample SQL**

Create `scripts/phase-07/32-retest-cursor-samples.sql`.

```sql
\if :{?user_id}
\else
  \set user_id 707000
\endif

\echo 'PHASE7_RETEST_CURSOR_SAMPLES'
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
```

- [ ] **Step 3: Create Cursor next-slice EXPLAIN SQL**

Create `scripts/phase-07/33-retest-cursor-next-slice-explain.sql`.

```sql
\if :{?user_id}
\else
  \set user_id 707000
\endif

\if :{?last_created_at}
\else
  \error 'last_created_at is required'
\endif

\if :{?last_id}
\else
  \error 'last_id is required'
\endif

\echo 'PHASE7_RETEST_CURSOR_NEXT_SLICE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
  AND (created_at, id) < (:'last_created_at'::timestamp, :last_id)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

- [ ] **Step 4: Create count-only EXPLAIN SQL**

Create `scripts/phase-07/34-retest-count-only-explain.sql`.

```sql
\if :{?user_id}
\else
  \set user_id 707000
\endif

\echo 'PHASE7_RETEST_COUNT_ONLY'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM point_history
WHERE user_id = :user_id;
```

- [ ] **Step 5: Run SQL scripts and save outputs**

Run from repository root:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/31-retest-offset-sampling-explain.sql
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/32-retest-cursor-samples.sql
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/34-retest-count-only-explain.sql
```

Save outputs to:

```text
docs/evidence/phase-07/explain/retest-offset-sampling.txt
docs/evidence/phase-07/data-profile/retest-cursor-samples.txt
docs/evidence/phase-07/explain/retest-count-only.txt
```

- [ ] **Step 6: Run Cursor next-slice EXPLAIN for representative pages**

Use values from `retest-cursor-samples.txt` for pages `10`, `1000`, and `4999`.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -v last_created_at='2026-05-26 18:26:41' -v last_id=707019999 -f /workspace/scripts/phase-07/33-retest-cursor-next-slice-explain.sql
```

Save outputs to:

```text
docs/evidence/phase-07/explain/retest-cursor-page10.txt
docs/evidence/phase-07/explain/retest-cursor-page1000.txt
docs/evidence/phase-07/explain/retest-cursor-page4999.txt
```

## Done When

- [ ] SQL scripts `31` through `34` exist.
- [ ] Cursor sample output includes all 10 sample pages.
- [ ] Cursor next-slice EXPLAIN does not include CTE `OFFSET` lookup.
- [ ] count-only EXPLAIN output is saved.

