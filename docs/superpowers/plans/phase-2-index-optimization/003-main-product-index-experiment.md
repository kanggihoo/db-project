# 003 Main Product Index Experiment

## Goal

Run the main Phase 2 experiment against the existing product API path and capture comparable post-index evidence.

## Files

- Read: `scripts/phase-02/00-clean-product-indexes.sql`
- Read: `scripts/phase-02/01-main-pre-index-explain.sql`
- Read: `scripts/phase-02/02-create-main-index.sql`
- Read: `scripts/phase-02/03-main-post-index-explain.sql`
- Read: `scripts/phase-02/04-product-pg-stat-statements.sql`
- Create: `docs/evidence/phase-02/products/pre-index/explain.txt`
- Create: `docs/evidence/phase-02/products/pool10-post-index/explain.txt`
- Create: `docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt`
- Create: `docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt`
- Create: `docs/evidence/phase-02/grafana-screenshots/products-post-index.png`
- Modify: `docs/evidence/phase-02/README.md`

## Steps

- [ ] **Step 1: Start infrastructure**

Run:

```bash
rtk docker compose up -d
rtk docker compose ps
```

Expected: `postgres`, `prometheus`, and `grafana` are `Up`. `postgres_exporter` should also be `Up` if enabled by compose.

- [ ] **Step 2: Ensure loadtest data exists**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT 'product' AS table_name, COUNT(*) AS rows FROM product;"
```

Expected: `product` row count is `100000`. If it is 0, run:

```bash
./scripts/seed.sh loadtest
```

- [ ] **Step 3: Start Spring server with pool10**

Run in a separate terminal:

```bash
./scripts/server.sh pool10
```

Expected: Spring Boot starts successfully and remains running on port 8080.

- [ ] **Step 4: Verify product API is reachable**

Run:

```bash
rtk curl -s "http://localhost:8080/api/products?categoryId=10&status=ON_SALE"
```

Expected: JSON array response. Empty array is acceptable; HTTP connection failure is not acceptable.

- [ ] **Step 5: Clean experimental indexes**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/00-clean-product-indexes.sql
```

Expected: command exits 0 and prints `ANALYZE`.

- [ ] **Step 6: Capture pre-index EXPLAIN**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/01-main-pre-index-explain.sql | tee docs/evidence/phase-02/products/pre-index/explain.txt
```

Expected: output contains `Phase 2 main product query: pre-index`, `EXPLAIN`, and a scan node such as `Seq Scan`.

- [ ] **Step 7: Create main index**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/02-create-main-index.sql
```

Expected: output contains `CREATE INDEX` or a notice that `idx_product_category_status` already exists, followed by `ANALYZE`.

- [ ] **Step 8: Capture post-index EXPLAIN**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/03-main-post-index-explain.sql | tee docs/evidence/phase-02/products/pool10-post-index/explain.txt
```

Expected: output contains `Phase 2 main product query: post-index`, `EXPLAIN`, and a scan node. Accept `Index Scan`, `Bitmap Index Scan`, or another planner choice, but record the actual result.

- [ ] **Step 9: Reset statistics before k6**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "VACUUM ANALYZE product;"
```

Expected: first command returns one row; second command returns `VACUUM`.

- [ ] **Step 10: Run post-index product k6 scenario**

Run:

```bash
PHASE=phase-02 POOL=pool10 K6_LOG_FILE=docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt ./k6/run.sh products baseline prometheus
```

Expected: k6 completes, full stdout/stderr is saved to `docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt`, and the console shows only the final tail of that log. The saved log contains `http_req_duration`, `http_req_failed`, and `dropped_iterations`.

- [ ] **Step 11: Capture post-index pg_stat_statements**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/04-product-pg-stat-statements.sql | tee docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

Expected: output contains the product query shape with `from product` and timing columns `mean_ms` and `total_ms`.

- [ ] **Step 12: Capture Grafana screenshot**

Open Grafana at `http://localhost:3000`, select `DB Lab Overview`, and set variables:

| Variable | Value |
|---|---|
| `$phase` | `phase-02` |
| `$scenario` | `products` |
| `$preset` | `baseline` |
| `$pool` | `pool10` |

Save the screenshot as:

```text
docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

Expected: screenshot shows product post-index run summary or table access panels.

- [ ] **Step 13: Verify main evidence files**

Run:

```bash
rtk grep "Phase 2 main product query: pre-index" docs/evidence/phase-02/products/pre-index/explain.txt
rtk grep "Phase 2 main product query: post-index" docs/evidence/phase-02/products/pool10-post-index/explain.txt
rtk grep "http_req_duration" docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
rtk grep "from product" docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

Expected: all commands return at least one matching line.

- [ ] **Step 14: Update evidence index**

In `docs/evidence/phase-02/README.md`, keep the main comparison table and add notes under it:

```markdown
## Main API Comparison Notes

- The pre-index EXPLAIN output records the representative product query before `idx_product_category_status`.
- The post-index EXPLAIN output records the same query after `idx_product_category_status`.
- The k6 and `pg_stat_statements` files use `phase=phase-02`, `scenario=products`, `preset=baseline`, and `pool=pool10`.
```

- [ ] **Step 15: Commit**

```bash
git add docs/evidence/phase-02/products docs/evidence/phase-02/grafana-screenshots docs/evidence/phase-02/README.md
git commit -m "docs: capture phase 2 main product index evidence"
```
