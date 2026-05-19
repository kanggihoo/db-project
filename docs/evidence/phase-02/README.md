# Phase 2 Evidence

이 디렉토리는 Phase 2 상품 검색 API의 인덱스 적용 전후 비교 Evidence와 SQL-only 보조 실험 Evidence를 저장한다.

## Main API Comparison

| Evidence | Path |
|---|---|
| Pre-index EXPLAIN | [products/pre-index/explain.txt](./products/pre-index/explain.txt) |
| Post-index EXPLAIN | [products/pool10-post-index/explain.txt](./products/pool10-post-index/explain.txt) |
| Post-index k6 summary | [products/pool10-post-index/k6-summary.txt](./products/pool10-post-index/k6-summary.txt) |
| Post-index pg_stat_statements | [products/pool10-post-index/pg-stat-statements.txt](./products/pool10-post-index/pg-stat-statements.txt) |
| Post-index Grafana screenshot | [grafana-screenshots/products-post-index.png](./grafana-screenshots/products-post-index.png) |

## SQL-only Auxiliary Experiments

| Experiment | Path |
|---|---|
| Single-column index selectivity | [sql-only/single-status-index.txt](./sql-only/single-status-index.txt) |
| Composite index order | [sql-only/composite-order-index.txt](./sql-only/composite-order-index.txt) |
| Covering index | [sql-only/covering-index.txt](./sql-only/covering-index.txt) |
| Partial index | [sql-only/partial-index.txt](./sql-only/partial-index.txt) |
