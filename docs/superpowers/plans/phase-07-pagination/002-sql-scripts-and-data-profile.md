# 002 SQL Scripts And Data Profile

## Goal

전체 `point_history` SQL-only 실험과 hot user SQL 실험을 재현하는 Phase 7 SQL 스크립트를 만든다.

## Files

- Create: `scripts/phase-07/00-data-profile.sql`
- Create: `scripts/phase-07/10-global-index-prepare.sql`
- Create: `scripts/phase-07/11-global-offset-page0-explain.sql`
- Create: `scripts/phase-07/12-global-offset-deep-explain.sql`
- Create: `scripts/phase-07/13-global-cursor-deep-explain.sql`
- Create: `scripts/phase-07/20-user-index-prepare.sql`
- Create: `scripts/phase-07/21-user-offset-page0-explain.sql`
- Create: `scripts/phase-07/22-user-offset-deep-explain.sql`
- Create: `scripts/phase-07/23-user-cursor-deep-explain.sql`
- Create: `scripts/phase-07/30-pg-stat-statements.sql`

## Steps

- [ ] **Step 1: Create data profile SQL**

Create `scripts/phase-07/00-data-profile.sql`:

```sql
\echo 'PHASE7_POINT_HISTORY_TOTAL_COUNT'
SELECT COUNT(*) AS point_history_count
FROM point_history;

\echo 'PHASE7_HOT_USER_POINT_COUNTS'
SELECT user_id, COUNT(*) AS point_count
FROM point_history
GROUP BY user_id
ORDER BY point_count DESC
LIMIT 20;

\echo 'PHASE7_INDEX_STATE_BEFORE'
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'point_history'
ORDER BY indexname;
```

- [ ] **Step 2: Create global index prepare SQL**

Create `scripts/phase-07/10-global-index-prepare.sql`:

```sql
DROP INDEX IF EXISTS idx_point_history_created_id;

CREATE INDEX idx_point_history_created_id
ON point_history (created_at DESC, id DESC);

VACUUM (ANALYZE) point_history;

SELECT pg_stat_statements_reset();
```

- [ ] **Step 3: Create global offset page0 EXPLAIN SQL**

Create `scripts/phase-07/11-global-offset-page0-explain.sql`:

```sql
\echo 'PHASE7_GLOBAL_OFFSET_PAGE0'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;
```

- [ ] **Step 4: Create global offset deep EXPLAIN SQL**

Create `scripts/phase-07/12-global-offset-deep-explain.sql`:

```sql
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
```

- [ ] **Step 5: Create global cursor deep EXPLAIN SQL**

Create `scripts/phase-07/13-global-cursor-deep-explain.sql`:

```sql
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
```

- [ ] **Step 6: Create hot user index prepare SQL**

Create `scripts/phase-07/20-user-index-prepare.sql`:

```sql
DROP INDEX IF EXISTS idx_point_history_user_created_id;

CREATE INDEX idx_point_history_user_created_id
ON point_history (user_id, created_at DESC, id DESC);

VACUUM (ANALYZE) point_history;

SELECT pg_stat_statements_reset();
```

- [ ] **Step 7: Create user offset page0 EXPLAIN SQL**

Create `scripts/phase-07/21-user-offset-page0-explain.sql`:

```sql
\if :{?user_id}
\else
  \set user_id 1
\endif

\echo 'PHASE7_USER_OFFSET_PAGE0'
\echo 'user_id=' :user_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :user_id
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;
```

- [ ] **Step 8: Create user offset deep EXPLAIN SQL**

Create `scripts/phase-07/22-user-offset-deep-explain.sql`:

```sql
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
```

- [ ] **Step 9: Create user cursor deep EXPLAIN SQL**

Create `scripts/phase-07/23-user-cursor-deep-explain.sql`:

```sql
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
```

- [ ] **Step 10: Create pg_stat_statements snapshot SQL**

Create `scripts/phase-07/30-pg-stat-statements.sql`:

```sql
\echo 'PHASE7_PG_STAT_STATEMENTS_POINT_HISTORY'
SELECT calls,
       ROUND(mean_exec_time::numeric, 2) AS mean_ms,
       ROUND(total_exec_time::numeric, 2) AS total_ms,
       rows,
       query
FROM pg_stat_statements
WHERE query ILIKE '%point_history%'
ORDER BY total_exec_time DESC
LIMIT 20;
```

- [ ] **Step 11: Verify script markers**

Run:

```bash
rtk rg -n "PHASE7_|idx_point_history_created_id|idx_point_history_user_created_id|EXPLAIN \\(ANALYZE, BUFFERS\\)" scripts/phase-07
```

Expected: all script markers and index names are found.

- [ ] **Step 12: Commit**

```bash
git add scripts/phase-07
git commit -m "test(phase7): add pagination sql evidence scripts"
```

