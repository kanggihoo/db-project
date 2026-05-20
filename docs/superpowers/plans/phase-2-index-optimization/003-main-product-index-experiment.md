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
- Create: `docs/evidence/phase-02/products/pool10-post-index/run-window.json`
- Create: `docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt`
- Create: `docs/evidence/phase-02/grafana-screenshots/products-pool10-post-index.png`
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

- [ ] **Step 10: Run post-index product k6 scenario and capture Grafana**

Run:

```bash
rtk npm run evidence:capture -- --phase phase-02 --scenario products --condition pool10-post-index --table product
```

Expected: k6 completes, full stdout/stderr is shown in the terminal and saved to `docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt`, `run-window.json` is saved next to the k6 log, and Grafana is captured to `docs/evidence/phase-02/grafana-screenshots/products-pool10-post-index.png`. The saved log contains `http_req_duration`, `http_req_failed`, and `dropped_iterations`.

If an AI agent runs this step and should avoid loading the full k6 output into context, add `K6_TAIL_ONLY=1`:

```bash
K6_TAIL_ONLY=1 rtk npm run evidence:capture -- --phase phase-02 --scenario products --condition pool10-post-index --table product
```

`run.sh` records host-side `startedAt` and `endedAt`, then stores `grafanaFrom=<startedAt-10s>` and `grafanaTo=<endedAt+20s>` in `run-window.json` next to `K6_LOG_FILE`. The capture step passes that exact `run-window.json` through `--window-file`, so it does not rely on searching for the latest matching run window.

- [ ] **Step 11: Capture post-index pg_stat_statements**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/04-product-pg-stat-statements.sql | tee docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

Expected: output contains the product query shape with `from product` and timing columns `mean_ms` and `total_ms`.

- [ ] **Step 12: Recapture fixed-window Grafana screenshot if needed**

Skip this step if Step 10 already produced the screenshot and the dashboard JSON did not change.

If dashboard JSON changed, regenerate it and restart Grafana first:

```bash
rtk node scripts/generate-db-lab-dashboard.mjs
rtk docker compose restart grafana
```

Run:

```bash
rtk npm run grafana:capture -- --phase phase-02 --scenario products --preset baseline --pool pool10 --table product --window-file docs/evidence/phase-02/products/pool10-post-index/run-window.json --output docs/evidence/phase-02/grafana-screenshots/products-pool10-post-index.png
```

Expected: `products-pool10-post-index.png` is captured through the fixed k6 run window from `docs/evidence/phase-02/products/pool10-post-index/run-window.json`. This reuses existing Prometheus data and does not rerun k6. Do not use a live `now-30m` dashboard URL for Phase evidence, because the final idle samples can make summary panels show `0`.

- [ ] **Step 13: Verify main evidence files**

Run:

```bash
rtk grep "Phase 2 main product query: pre-index" docs/evidence/phase-02/products/pre-index/explain.txt
rtk grep "Phase 2 main product query: post-index" docs/evidence/phase-02/products/pool10-post-index/explain.txt
rtk grep "http_req_duration" docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
rtk grep "grafanaFrom" docs/evidence/phase-02/products/pool10-post-index/run-window.json
rtk grep "from product" docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
rtk powershell -NoProfile -Command "Test-Path 'docs/evidence/phase-02/grafana-screenshots/products-pool10-post-index.png'"
```

Expected: all commands return at least one matching line.

- [ ] **Step 14: Update evidence index**

In `docs/evidence/phase-02/README.md`, keep the main comparison table and add notes under it:

```markdown
## Main API Comparison Notes

- The pre-index EXPLAIN output records the representative product query before `idx_product_category_status`.
- The post-index EXPLAIN output records the same query after `idx_product_category_status`.
- The k6 and `pg_stat_statements` files use `phase=phase-02`, `scenario=products`, `preset=baseline`, and `pool=pool10`.
- The Grafana screenshot was captured with the fixed `run-window.json` time range, not a live `now-30m` window.
```

- [ ] **Step 15: Commit**

```bash
git add docs/evidence/phase-02/products docs/evidence/phase-02/grafana-screenshots docs/evidence/phase-02/README.md
git commit -m "docs: capture phase 2 main product index evidence"
```
