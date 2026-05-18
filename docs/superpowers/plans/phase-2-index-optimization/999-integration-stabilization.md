# 999 Integration Stabilization

## Goal

Verify Phase 2 plan outputs, documentation, SQL scripts, and evidence references are consistent after all implementation slices are complete.

## Files

- No planned source changes.
- Modify only the file that fails a verification step, then rerun the same step.

## Steps

- [ ] **Step 1: Verify Phase 2 docs exist**

Run:

```bash
rtk find docs/phases/02-indexes -maxdepth 1 -type f
```

Expected output includes:

```text
README.md
scope.md
runbook.md
observability.md
report.md
```

- [ ] **Step 2: Verify SQL scripts exist**

Run:

```bash
rtk find scripts/phase-02 -maxdepth 1 -type f
```

Expected output includes:

```text
00-clean-product-indexes.sql
01-main-pre-index-explain.sql
02-create-main-index.sql
03-main-post-index-explain.sql
04-product-pg-stat-statements.sql
10-single-status-index.sql
20-composite-order-index.sql
30-covering-index.sql
40-partial-index.sql
```

- [ ] **Step 3: Verify SQL script content**

Run:

```bash
rtk grep "idx_product_category_status" scripts/phase-02
rtk grep "EXPLAIN (ANALYZE, BUFFERS)" scripts/phase-02
rtk grep "pg_stat_statements" scripts/phase-02/04-product-pg-stat-statements.sql
```

Expected: all commands return matches.

- [ ] **Step 4: Verify evidence files exist**

Run after executing slices 003 and 004:

```bash
rtk find docs/evidence/phase-02 -maxdepth 3 -type f
```

Expected output includes:

```text
products/pre-index/explain.txt
products/pool10-post-index/explain.txt
products/pool10-post-index/k6-summary.txt
products/pool10-post-index/pg-stat-statements.txt
sql-only/single-status-index.txt
sql-only/composite-order-index.txt
sql-only/covering-index.txt
sql-only/partial-index.txt
```

- [ ] **Step 5: Verify report is finalized**

Run:

```bash
rtk grep "measured from\\|<actual\\|측정 전이다" docs/phases/02-indexes/report.md
```

Expected: zero matches.

- [ ] **Step 6: Verify README links**

Run:

```bash
rtk grep "Phase 2 Index Optimization Spec" docs/superpowers/README.md
rtk grep "Phase 2 Index Optimization Plan" docs/superpowers/README.md
rtk grep "docs/evidence/phase-02/README.md" docs/phases/02-indexes/README.md
```

Expected: all commands return matches.

- [ ] **Step 7: Optional stress evidence**

Only run if the main baseline comparison is too small to show a visible API-level difference:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
PHASE=phase-02 POOL=pool10 ./k6/run.sh products stress-100 prometheus | tee docs/evidence/phase-02/products/pool10-post-index/k6-stress-100-summary.txt
```

Expected: k6 completes and output contains `http_req_duration`.

- [ ] **Step 8: Final git status**

Run:

```bash
rtk git status
```

Expected: only intentional Phase 2 files are modified or untracked.

- [ ] **Step 9: Commit stabilization fixes if needed**

Only run if this stabilization slice required source changes:

```bash
git add <changed-files>
git commit -m "fix: stabilize phase 2 index documentation"
```
