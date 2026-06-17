# Phase 6 Aggregation Design

## Goal

Phase 6 measures how PostgreSQL index design affects aggregation query plans.

This phase compares `baseline`, `naive index`, and `query-shaped index` conditions for two representative aggregation targets:

- Product Review Summary: `product` + `review`
- Monthly Order Aggregate: `orders` + `users`

The primary evidence is SQL-only `EXPLAIN (ANALYZE, BUFFERS)` output captured through `psql`. k6/Grafana evidence is limited to one representative API, Product Review Summary, and compares only `naive index` versus `query-shaped index`.

## Scope

Phase 6 includes:

- Data profile evidence for the seed data used by the aggregation experiments.
- Numbered SQL scripts under `scripts/phase-06/`.
- SQL-only plan comparison for Product Review Summary across three index conditions.
- SQL-only plan comparison for Monthly Order Aggregate across three index conditions.
- A simple measurement endpoint: `GET /api/products/review-summary`.
- k6 evidence for the Product Review Summary API under `naive index` and `query-shaped index` conditions.
- Phase documentation and evidence indexes under the standard Phase structure.

Phase 6 excludes:

- Adding final aggregation indexes to `docker/postgres/init.sql`.
- General-purpose product statistics APIs.
- User-facing filtering or pagination for the review summary endpoint.
- QueryDSL implementation for the aggregation endpoint.
- Treating k6 as primary evidence for every SQL condition.
- Phase 7 pagination optimization.

## Measurement Conditions

The reference environment is docker compose PostgreSQL with the `loadtest` seed preset.

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

`baseline` means the seed database has no Phase 6 custom experiment indexes. PostgreSQL primary key and unique indexes remain in place.

Experiment indexes are created and dropped only by Phase 6 prepare scripts. They are not written into the base schema.

## Data Profile Evidence

Before plan comparisons, capture data profile evidence under `docs/evidence/phase-06/data-profile/`.

Required profile:

- row counts for `product`, `review`, `orders`, and `users`
- reviewed product count
- product-level review count min, max, average, and top 20
- monthly order count and revenue distribution
- current index state for `product`, `review`, `orders`, and `users`

The profile explains why the planner picked a plan for this data distribution.

## SQL Script Structure

SQL-only evidence uses numbered scripts under `scripts/phase-06/`.

```text
scripts/phase-06/
  00-data-profile.sql
  10-review-baseline-prepare.sql
  11-review-baseline-explain.sql
  12-review-naive-prepare.sql
  13-review-naive-explain.sql
  14-review-query-shaped-prepare.sql
  15-review-query-shaped-explain.sql
  20-monthly-baseline-prepare.sql
  21-monthly-baseline-explain.sql
  22-monthly-naive-prepare.sql
  23-monthly-naive-explain.sql
  24-monthly-query-shaped-prepare.sql
  25-monthly-query-shaped-explain.sql
```

Each `prepare` script:

- removes custom indexes for the same experiment target
- creates the indexes required for that condition
- runs `VACUUM (ANALYZE)` for the target tables
- runs `pg_stat_statements_reset()` so query-level snapshots can be separated by condition

Each `explain` script contains only the target `EXPLAIN (ANALYZE, BUFFERS)` query.

Run `VACUUM` through standalone `psql -f` execution because PostgreSQL does not allow `VACUUM` inside a transaction block.

## Product Review Summary Experiment

Representative SQL:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id,
       p.name,
       ROUND(AVG(r.rating), 2) AS avg_rating,
       COUNT(r.id) AS review_count
FROM product p
LEFT JOIN review r ON r.product_id = p.id
GROUP BY p.id, p.name
HAVING COUNT(r.id) >= 10
ORDER BY AVG(r.rating) DESC, p.id ASC
LIMIT 100;
```

Conditions:

| Condition | Index |
|---|---|
| baseline | no Phase 6 custom index |
| naive index | `CREATE INDEX idx_review_product_id ON review(product_id);` |
| query-shaped index | `CREATE INDEX idx_review_product_rating ON review(product_id, rating);` |

Evidence should explain:

- scan path for `review`
- join path between `product` and `review`
- whether `HAVING COUNT(r.id) >= 10` reduces work before or after aggregation
- whether `ORDER BY AVG(r.rating)` still requires a sort
- whether PostgreSQL picks `HashAggregate` or `GroupAggregate`

## Monthly Order Aggregate Experiment

Representative SQL:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT u.grade,
       DATE_TRUNC('month', o.created_at) AS order_month,
       COUNT(o.id) AS order_count,
       SUM(o.final_price) AS total_revenue
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY order_month DESC;
```

Conditions:

| Condition | Index |
|---|---|
| baseline | no Phase 6 custom index |
| naive index | `CREATE INDEX idx_orders_created_at ON orders(created_at);` |
| query-shaped index | `CREATE INDEX idx_orders_month_user ON orders ((DATE_TRUNC('month', created_at)), user_id);` |

The expression index must be verified in the target PostgreSQL environment. If PostgreSQL rejects the index or the planner does not use it, the evidence records that result instead of forcing a plan.

Evidence should explain:

- whether a plain `orders(created_at)` index helps a `DATE_TRUNC` group key
- whether the expression index changes scan, sort, or aggregate nodes
- whether PostgreSQL picks `HashAggregate` or `GroupAggregate`
- how execution time relates to buffer usage

## Product Review Summary API

Add one measurement endpoint:

```http
GET /api/products/review-summary
```

This endpoint has no request parameters in Phase 6.

Behavior:

- execute the Product Review Summary query shape
- return the top 100 rows ordered by average rating descending and `productId` ascending as a stable tie-breaker
- use fixed `HAVING COUNT(r.id) >= 10`
- return a DTO with `productId`, `productName`, `avgRating`, and `reviewCount`

Implementation should prefer a simple native SQL path, such as `JdbcTemplate`, so the API query shape stays close to the SQL-only evidence. QueryDSL is intentionally outside this endpoint because Phase 6 compares PostgreSQL aggregation plans, not application query builders.

## k6 Evidence

k6 targets only the Product Review Summary API.

Required k6 conditions:

- `naive index`
- `query-shaped index`

Do not run k6 for the SQL-only baseline. The baseline is captured as DB plan evidence. The API evidence answers whether the practical candidate indexes affect HTTP p95/p99.

k6 evidence should include:

- summary output
- run window
- Grafana screenshot if the run uses Prometheus mode
- clear condition naming in output paths

## Evidence Structure

```text
docs/evidence/phase-06/
  README.md
  data-profile/
    row-counts.txt
    review-distribution.txt
    monthly-order-distribution.txt
    index-state-before.txt
  review-aggregate/
    baseline/
      explain.txt
    naive-index/
      index-ddl.sql
      explain.txt
    query-shaped-index/
      index-ddl.sql
      explain.txt
  monthly-order-aggregate/
    baseline/
      explain.txt
    naive-index/
      index-ddl.sql
      explain.txt
    query-shaped-index/
      index-ddl.sql
      explain.txt
  review-summary-api/
    naive-index/
      k6-summary.txt
      run-window.json
    query-shaped-index/
      k6-summary.txt
      run-window.json
  grafana-screenshots/
    review-summary-naive-index.png
    review-summary-query-shaped-index.png
```

## Testing

Code tests should verify the API behavior, not the final database plan.

Required checks:

- `GET /api/products/review-summary` returns 200.
- response DTO contains `productId`, `productName`, `avgRating`, and `reviewCount`.
- returned rows are limited to 100.
- returned rows satisfy `reviewCount >= 10` for the test fixture.
- ordering is deterministic for equal average rating through the `productId` tie-breaker.

SQL scripts are verified by running them against docker compose PostgreSQL and checking that each expected evidence file is produced.

## Documentation Updates

Create or update:

- `docs/roadmap/07-phase-6-aggregation.md`
- `docs/phases/06-aggregation/README.md`
- `docs/phases/06-aggregation/scope.md`
- `docs/phases/06-aggregation/runbook.md`
- `docs/phases/06-aggregation/observability.md`
- `docs/phases/06-aggregation/report.md`
- `docs/evidence/phase-06/README.md`
- `docs/guides/k6-load-testing.md` if a new k6 scenario or preset is added

Do not create extra files under `docs/phases/06-aggregation/` beyond the standard five phase files.

## Decisions

- Product Review Summary API is a measurement endpoint, not a generalized product reporting feature.
- The API has no request parameters in Phase 6.
- SQL-only `EXPLAIN (ANALYZE, BUFFERS)` is primary evidence.
- k6/Grafana is representative API evidence only.
- Phase 6 custom indexes live only in `scripts/phase-06/*-prepare.sql`.
- `docker/postgres/init.sql` is not changed for Phase 6 experiment indexes.
- `prepare` scripts are responsible for custom index state, `VACUUM (ANALYZE)`, and `pg_stat_statements_reset()`.
