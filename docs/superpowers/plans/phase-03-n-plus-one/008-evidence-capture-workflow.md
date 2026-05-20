# 008 Evidence Capture Workflow

## Goal

Add repeatable SQL helpers and runbook steps for collecting Phase 3 strategy evidence.

## Files

- Create: `scripts/phase-03/00-reset-statistics.sql`
- Create: `scripts/phase-03/01-pg-stat-statements.sql`
- Create: `scripts/phase-03/02-order-shape.sql`
- Create: `scripts/phase-03/10-lazy-explain.sql`
- Create: `scripts/phase-03/20-fetch-join-explain.sql`
- Create: `scripts/phase-03/30-batch-size-explain.sql`
- Create: `scripts/phase-03/40-entity-graph-explain.sql`
- Modify: `docs/phases/03-n-plus-one/runbook.md`
- Modify: `docs/evidence/phase-03/README.md`

## Steps

- [ ] **Step 1: Create scripts directory**

Run:

```bash
rtk proxy mkdir -p scripts/phase-03
```

Expected: command exits 0.

- [ ] **Step 2: Create reset script**

Create `scripts/phase-03/00-reset-statistics.sql`:

```sql
SELECT pg_stat_statements_reset();
VACUUM ANALYZE orders;
VACUUM ANALYZE order_item;
VACUUM ANALYZE product_sku;
VACUUM ANALYZE product;
VACUUM ANALYZE product_image;
```

- [ ] **Step 3: Create pg_stat_statements snapshot script**

Create `scripts/phase-03/01-pg-stat-statements.sql`:

```sql
SELECT calls,
       round(mean_exec_time::numeric, 2) AS mean_ms,
       round(total_exec_time::numeric, 2) AS total_ms,
       rows,
       left(query, 220) AS query
FROM pg_stat_statements
WHERE query ILIKE '%orders%'
   OR query ILIKE '%order_item%'
   OR query ILIKE '%product_sku%'
   OR query ILIKE '%product_image%'
   OR query ILIKE '% from product %'
ORDER BY total_exec_time DESC
LIMIT 30;
```

- [ ] **Step 4: Create order shape snapshot script**

Create `scripts/phase-03/02-order-shape.sql`:

```sql
\if :{?user_id}
\else
\set user_id 100
\endif

\echo 'Phase 3 selected order-list shape'
SELECT COUNT(DISTINCT o.id) AS order_count,
       COUNT(oi.id) AS order_item_count,
       COUNT(DISTINCT oi.sku_id) AS distinct_sku_count,
       COUNT(DISTINCT ps.product_id) AS distinct_product_count
FROM orders o
JOIN order_item oi ON oi.order_id = o.id
JOIN product_sku ps ON ps.id = oi.sku_id
WHERE o.user_id = :user_id;
```

This snapshot explains why Lazy repeated-select counts can differ from the simplified `1 + N + 3M` formula when SKUs or Products repeat inside the same persistence context.

- [ ] **Step 5: Create representative EXPLAIN scripts**

Create `scripts/phase-03/10-lazy-explain.sql`:

```sql
\echo 'Phase 3 lazy representative query: order_item by order_id'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, option_info, order_id, product_name, quantity, sku_id, status, unit_price
FROM order_item
WHERE order_id = 100;
```

Create `scripts/phase-03/20-fetch-join-explain.sql`:

```sql
\echo 'Phase 3 fetch-join representative query'
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.id, o.user_id, oi.id AS order_item_id, sku.id AS sku_id, p.id AS product_id
FROM orders o
JOIN order_item oi ON oi.order_id = o.id
JOIN product_sku sku ON sku.id = oi.sku_id
JOIN product p ON p.id = sku.product_id
WHERE o.user_id = 100;
```

Create `scripts/phase-03/30-batch-size-explain.sql`:

```sql
\echo 'Phase 3 batch-size representative query: order_item IN order_ids'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, option_info, order_id, product_name, quantity, sku_id, status, unit_price
FROM order_item
WHERE order_id IN (100, 101, 102);
```

Create `scripts/phase-03/40-entity-graph-explain.sql`:

```sql
\echo 'Phase 3 entity-graph representative query'
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.id, o.user_id, oi.id AS order_item_id, sku.id AS sku_id, p.id AS product_id
FROM orders o
LEFT JOIN order_item oi ON oi.order_id = o.id
LEFT JOIN product_sku sku ON sku.id = oi.sku_id
LEFT JOIN product p ON p.id = sku.product_id
WHERE o.user_id = 100;
```

- [ ] **Step 6: Add strategy runbook commands**

In `docs/phases/03-n-plus-one/runbook.md`, add:

````markdown
## Strategy evidence capture

For each strategy, reset statistics, run k6, then capture `pg_stat_statements`. Use the default server profile for `lazy`, `fetch-join`, and `entity-graph`; restart the server with `phase3-batch` only for `batch-size`.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/00-reset-statistics.sql
PHASE=phase-03 POOL=pool10 STRATEGY=lazy K6_LOG_FILE=docs/evidence/phase-03/orders/lazy/k6-summary.txt ./k6/run.sh orders baseline prometheus
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/01-pg-stat-statements.sql | tee docs/evidence/phase-03/orders/lazy/pg-stat-statements.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce -v user_id=100 < scripts/phase-03/02-order-shape.sql | tee docs/evidence/phase-03/orders/lazy/sql-count.txt
```

Repeat with `STRATEGY=fetch-join` and `STRATEGY=entity-graph` under the default server profile, changing the output directory to match the strategy.

For BatchSize, restart the server with the isolated profile before running the same capture sequence:

```bash
./scripts/server.sh phase3-batch
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/00-reset-statistics.sql
PHASE=phase-03 POOL=pool10 STRATEGY=batch-size K6_LOG_FILE=docs/evidence/phase-03/orders/batch-size/k6-summary.txt ./k6/run.sh orders baseline prometheus
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/01-pg-stat-statements.sql | tee docs/evidence/phase-03/orders/batch-size/pg-stat-statements.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce -v user_id=100 < scripts/phase-03/02-order-shape.sql | tee docs/evidence/phase-03/orders/batch-size/sql-count.txt
```

## Representative EXPLAIN

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/10-lazy-explain.sql | tee docs/evidence/phase-03/orders/lazy/explain.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/20-fetch-join-explain.sql | tee docs/evidence/phase-03/orders/fetch-join/explain.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/30-batch-size-explain.sql | tee docs/evidence/phase-03/orders/batch-size/explain.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/40-entity-graph-explain.sql | tee docs/evidence/phase-03/orders/entity-graph/explain.txt
```
````

- [ ] **Step 7: Verify scripts exist and contain markers**

Run:

```bash
rtk proxy find scripts/phase-03 -maxdepth 1 -type f | sort
rtk grep "Phase 3 lazy representative query" scripts/phase-03/10-lazy-explain.sql
rtk grep "pg_stat_statements_reset" scripts/phase-03/00-reset-statistics.sql
rtk grep "distinct_sku_count" scripts/phase-03/02-order-shape.sql
```

Expected: all scripts exist and grep commands return matches.

- [ ] **Step 8: Commit**

```bash
git add scripts/phase-03 docs/phases/03-n-plus-one/runbook.md docs/evidence/phase-03/README.md
git commit -m "docs: add phase 3 evidence capture workflow"
```
