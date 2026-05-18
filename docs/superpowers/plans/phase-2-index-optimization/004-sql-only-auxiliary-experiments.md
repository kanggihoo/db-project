# 004 SQL-only Auxiliary Experiments

## Goal

Run SQL-only experiments that explain index behavior without changing the main API comparison.

## Files

- Read: `scripts/phase-02/10-single-status-index.sql`
- Read: `scripts/phase-02/20-composite-order-index.sql`
- Read: `scripts/phase-02/30-covering-index.sql`
- Read: `scripts/phase-02/40-partial-index.sql`
- Create: `docs/evidence/phase-02/sql-only/single-status-index.txt`
- Create: `docs/evidence/phase-02/sql-only/composite-order-index.txt`
- Create: `docs/evidence/phase-02/sql-only/covering-index.txt`
- Create: `docs/evidence/phase-02/sql-only/partial-index.txt`
- Modify: `docs/evidence/phase-02/README.md`
- Modify: `docs/phases/02-indexes/report.md`

## Steps

- [ ] **Step 1: Run single-column status index experiment**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/10-single-status-index.sql | tee docs/evidence/phase-02/sql-only/single-status-index.txt
```

Expected: output contains `Before status index`, `After status index`, and two `EXPLAIN` results.

- [ ] **Step 2: Run composite order experiment**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/20-composite-order-index.sql | tee docs/evidence/phase-02/sql-only/composite-order-index.txt
```

Expected: output contains all three labels:

```text
category_id only with ORDER BY created_at
category_id and status with ORDER BY created_at
category_id with created_at range
```

- [ ] **Step 3: Run covering index experiment**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/30-covering-index.sql | tee docs/evidence/phase-02/sql-only/covering-index.txt
```

Expected: output contains `Before covering index`, `After covering index`, and two `EXPLAIN` results. If `Index Only Scan` does not appear, record the actual plan and explain visibility map or planner choice in the report.

- [ ] **Step 4: Run partial index experiment**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/40-partial-index.sql | tee docs/evidence/phase-02/sql-only/partial-index.txt
```

Expected: output contains `Before partial index`, `After partial index`, and two `EXPLAIN` results.

- [ ] **Step 5: Verify SQL-only evidence files**

Run:

```bash
rtk grep "Before status index" docs/evidence/phase-02/sql-only/single-status-index.txt
rtk grep "category_id and status with ORDER BY created_at" docs/evidence/phase-02/sql-only/composite-order-index.txt
rtk grep "After covering index" docs/evidence/phase-02/sql-only/covering-index.txt
rtk grep "After partial index" docs/evidence/phase-02/sql-only/partial-index.txt
```

Expected: all commands return at least one matching line.

- [ ] **Step 6: Update evidence index**

In `docs/evidence/phase-02/README.md`, add:

```markdown
## SQL-only Notes

- SQL-only evidence is captured through `psql`, not the product API.
- These files explain index behavior and are not compared directly with k6 latency.
- Each SQL-only script cleans unrelated experimental indexes before running its own experiment.
```

- [ ] **Step 7: Add SQL-only summary to report**

In `docs/phases/02-indexes/report.md`, replace the SQL-only section with:

```markdown
## SQL-only 보조 실험 결과

| Topic | Evidence | Interpretation |
|---|---|---|
| Single status index | [single-status-index.txt](../../evidence/phase-02/sql-only/single-status-index.txt) | `status = 'ON_SALE'`의 선택도가 낮아 planner가 Seq Scan을 선택할 수 있음을 확인한다. |
| Composite index order | [composite-order-index.txt](../../evidence/phase-02/sql-only/composite-order-index.txt) | 동등 조건만으로는 순서 차이가 작을 수 있고, 단독 조건, range 조건, `ORDER BY`에서 차이가 커질 수 있음을 확인한다. |
| Covering index | [covering-index.txt](../../evidence/phase-02/sql-only/covering-index.txt) | 필요한 컬럼만 조회할 때 인덱스가 테이블 접근을 줄일 수 있는지 확인한다. |
| Partial index | [partial-index.txt](../../evidence/phase-02/sql-only/partial-index.txt) | `is_deleted = false` 조건이 있을 때 부분 인덱스가 실행계획에 미치는 영향을 확인한다. |
```

- [ ] **Step 8: Commit**

```bash
git add docs/evidence/phase-02/sql-only docs/evidence/phase-02/README.md docs/phases/02-indexes/report.md
git commit -m "docs: capture phase 2 sql-only index experiments"
```
