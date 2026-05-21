# 002 SQL Script Kit

## Goal

Create repeatable SQL scripts for Phase 2 index cleanup, main pre/post explain, k6 query snapshot, and SQL-only auxiliary experiments.

## Files

- Create: `scripts/phase-02/00-clean-product-indexes.sql`
- Create: `scripts/phase-02/01-main-pre-index-explain.sql`
- Create: `scripts/phase-02/02-create-main-index.sql`
- Create: `scripts/phase-02/03-main-post-index-explain.sql`
- Create: `scripts/phase-02/04-product-pg-stat-statements.sql`
- Create: `scripts/phase-02/10-single-status-index.sql`
- Create: `scripts/phase-02/20-composite-order-index.sql`
- Create: `scripts/phase-02/30-covering-index.sql`
- Create: `scripts/phase-02/40-partial-index.sql`
- Modify: `docs/guides/scripts.md`

## Steps

- [ ] **Step 1: Create script directory**

Run:

```bash
rtk proxy mkdir -p scripts/phase-02
```

Expected: command exits 0.

- [ ] **Step 2: Create cleanup script**

Create `scripts/phase-02/00-clean-product-indexes.sql`:

```sql
DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

ANALYZE product;
```

- [ ] **Step 3: Create main pre-index EXPLAIN script**

Create `scripts/phase-02/01-main-pre-index-explain.sql`:

```sql
\echo 'Phase 2 main product query: pre-index'

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';
```

- [ ] **Step 4: Create main index script**

Create `scripts/phase-02/02-create-main-index.sql`:

```sql
CREATE INDEX IF NOT EXISTS idx_product_category_status
  ON product(category_id, status);

ANALYZE product;
```

- [ ] **Step 5: Create main post-index EXPLAIN script**

Create `scripts/phase-02/03-main-post-index-explain.sql`:

```sql
\echo 'Phase 2 main product query: post-index'

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';
```

- [ ] **Step 6: Create product pg_stat_statements snapshot script**

Create `scripts/phase-02/04-product-pg-stat-statements.sql`:

```sql
SELECT
  calls,
  ROUND(mean_exec_time::numeric, 2) AS mean_ms,
  ROUND(total_exec_time::numeric, 2) AS total_ms,
  rows,
  query
FROM pg_stat_statements
WHERE query ILIKE '%from product%'
ORDER BY total_exec_time DESC
LIMIT 20;
```

- [ ] **Step 7: Create single-column selectivity script**

Create `scripts/phase-02/10-single-status-index.sql`:

```sql
\echo 'SQL-only: single-column status index selectivity'

DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

ANALYZE product;

\echo 'Before status index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE status = 'ON_SALE';

CREATE INDEX idx_product_status
  ON product(status);

ANALYZE product;

\echo 'After status index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE status = 'ON_SALE';
```

- [ ] **Step 8: Create composite order script**

Create `scripts/phase-02/20-composite-order-index.sql`:

```sql
\echo 'SQL-only: composite index order'

DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

CREATE INDEX idx_product_category_status_created
  ON product(category_id, status, created_at DESC);

CREATE INDEX idx_product_status_category_created
  ON product(status, category_id, created_at DESC);

ANALYZE product;

\echo 'category_id only with ORDER BY created_at'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
ORDER BY created_at DESC;

\echo 'category_id and status with ORDER BY created_at'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE'
ORDER BY created_at DESC;

\echo 'category_id with created_at range'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND created_at >= TIMESTAMP '2026-01-01 00:00:00';
```

- [ ] **Step 9: Create covering index script**

Create `scripts/phase-02/30-covering-index.sql`:

```sql
\echo 'SQL-only: covering index'

DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

ANALYZE product;

\echo 'Before covering index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';

CREATE INDEX idx_product_covering
  ON product(category_id, status)
  INCLUDE (id, name, base_price);

VACUUM ANALYZE product;

\echo 'After covering index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';
```

- [ ] **Step 10: Create partial index script**

Create `scripts/phase-02/40-partial-index.sql`:

```sql
\echo 'SQL-only: partial index for active products'

DROP INDEX IF EXISTS idx_product_category_status;
DROP INDEX IF EXISTS idx_product_status;
DROP INDEX IF EXISTS idx_product_category_status_created;
DROP INDEX IF EXISTS idx_product_status_category_created;
DROP INDEX IF EXISTS idx_product_covering;
DROP INDEX IF EXISTS idx_product_active_category_status;

ANALYZE product;

\echo 'Before partial index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE'
  AND is_deleted = false;

CREATE INDEX idx_product_active_category_status
  ON product(category_id, status)
  WHERE is_deleted = false;

ANALYZE product;

\echo 'After partial index'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE'
  AND is_deleted = false;
```

- [ ] **Step 11: Update `docs/guides/scripts.md`**

Add this section after `scripts/server.sh`:

```markdown
## scripts/phase-02/

Phase 2 인덱스 실험용 SQL 스크립트다. 운영 migration이 아니라 Learning Phase evidence를 반복 생성하기 위한 도구다.

주요 스크립트:

| Script | Purpose |
|---|---|
| `00-clean-product-indexes.sql` | product 실험 인덱스 정리 |
| `01-main-pre-index-explain.sql` | 메인 상품 검색 pre-index 실행계획 |
| `02-create-main-index.sql` | `idx_product_category_status` 생성 |
| `03-main-post-index-explain.sql` | 메인 상품 검색 post-index 실행계획 |
| `04-product-pg-stat-statements.sql` | product query snapshot |
| `10-single-status-index.sql` | 단일 status 인덱스 선택도 실험 |
| `20-composite-order-index.sql` | 복합 인덱스 순서 실험 |
| `30-covering-index.sql` | 커버링 인덱스 실험 |
| `40-partial-index.sql` | soft delete 부분 인덱스 실험 |
```

- [ ] **Step 12: Verify SQL script kit**

Run:

```bash
rtk find scripts/phase-02 -maxdepth 1 -type f
rtk grep "idx_product_category_status" scripts/phase-02
rtk grep "EXPLAIN (ANALYZE, BUFFERS)" scripts/phase-02
```

Expected:

- 9 SQL files exist under `scripts/phase-02/`.
- `idx_product_category_status` appears in cleanup, create, and auxiliary scripts.
- Every explain script contains `EXPLAIN (ANALYZE, BUFFERS)`.

- [ ] **Step 13: Commit**

```bash
git add scripts/phase-02 docs/guides/scripts.md
git commit -m "docs: add phase 2 index experiment scripts"
```
