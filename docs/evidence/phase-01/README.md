# Phase-01 Evidence Index

## Environment evidence
- `db/before-measurement-state.txt`
- `db/after-measurement-state.txt`

## API smoke evidence
- `api-smoke/spring-health.json`
- `api-smoke/products-baseline.json`
- `api-smoke/orders-lazy-user950.json`
- `api-smoke/points-page0-user5714.json`
- `api-smoke/points-page500-user5714.json`

## SQL execution plans
- `explain/products-baseline-explain.txt`
- `explain/orders-lazy-explain.txt`
- `explain/points-offset-explain.txt`

## k6 evidence
- `products/products-baseline/measurement.json`
- `products/products-baseline/k6-summary.json`
- `products/products-baseline/k6-exit-status.txt`
- `products/products-baseline/run-window.json`
- `orders/orders-baseline/measurement.json`
- `orders/orders-baseline/k6-summary.json`
- `orders/orders-baseline/k6-exit-status.txt`
- `orders/orders-baseline/run-window.json`
- `points/points-page0/measurement.json`
- `points/points-page0/k6-summary.json`
- `points/points-page0/k6-exit-status.txt`
- `points/points-page0/run-window.json`
- `points/points-page500/measurement.json`
- `points/points-page500/k6-summary.json`
- `points/points-page500/k6-exit-status.txt`
- `points/points-page500/run-window.json`
- `rps20-test/products-baseline-rps20/measurement.json`
- `rps20-test/products-baseline-rps20/k6-summary.json`
- `rps20-test/products-baseline-rps20/k6-exit-status.txt`
- `rps20-test/products-baseline-rps20/run-window.json`
- `rps20-test/points-page0-rps20/measurement.json`
- `rps20-test/points-page0-rps20/k6-summary.json`
- `rps20-test/points-page0-rps20/k6-exit-status.txt`
- `rps20-test/points-page0-rps20/run-window.json`

## pg_stat_statements
- `products/products-baseline/pg-stat-statements.txt`
- `orders/orders-baseline/pg-stat-statements.txt`
- `points/page0/pg-stat-statements.txt`
- `points/page500/pg-stat-statements.txt`
- `rps20-test/points-page0-rps20/pg-stat-statements.txt`

## Notes
- `points/points-page0/measurement.json` and `points/points-page500/measurement.json` are retained as failed 50 rps runs, but both were executed with `preset=baseline` and weighted page selection. They are not valid evidence for a precise page0 vs page500 comparison.
- `rps20-test/points-page0-rps20` is the valid fixed page0 rerun used to support the report conclusion that the no-index points endpoint reaches an operational limit from page0.
