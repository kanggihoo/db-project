# Phase 2 Evidence

이 디렉토리는 Phase 2 상품 검색 API의 인덱스 적용 전후 비교 Evidence와 SQL-only 보조 실험 Evidence를 저장한다.

## Measurement Condition

| 항목 | 값 |
|---|---|
| phase | `phase-02` |
| scenario | `products` |
| preset | `baseline` |
| condition | `pool10-post-index` |
| pool | `pool10` |
| seed | `loadtest` |
| API | `GET /api/products?categoryId=&status=&strategy=baseline` |
| main index | `idx_product_category_status ON product(category_id, status)` |

## Expected Structure

| Evidence | Path |
|---|---|
| Seed/loadtest DB state | [db-state/00-seed-loadtest-state.txt](./db-state/00-seed-loadtest-state.txt) |
| Pre-index product state | [db-state/10-pre-index-product-state.txt](./db-state/10-pre-index-product-state.txt) |
| Post-index product state | [db-state/20-post-index-product-state.txt](./db-state/20-post-index-product-state.txt) |
| Pre-index EXPLAIN | [products/pre-index/explain.txt](./products/pre-index/explain.txt) |
| Post-index EXPLAIN | [products/pool10-post-index/explain.txt](./products/pool10-post-index/explain.txt) |
| k6 measurement manifest | [products/pool10-post-index/measurement.json](./products/pool10-post-index/measurement.json) |
| k6 summary | [products/pool10-post-index/k6-summary.json](./products/pool10-post-index/k6-summary.json) |
| k6 stdout summary | [products/pool10-post-index/k6-summary.txt](./products/pool10-post-index/k6-summary.txt) |
| k6 exit status | [products/pool10-post-index/k6-exit-status.txt](./products/pool10-post-index/k6-exit-status.txt) |
| k6 run window | [products/pool10-post-index/run-window.json](./products/pool10-post-index/run-window.json) |
| Post-index pg_stat_statements | [products/pool10-post-index/pg-stat-statements.txt](./products/pool10-post-index/pg-stat-statements.txt) |
| Post-index Grafana screenshot | [grafana-screenshots/products-post-index.png](./grafana-screenshots/products-post-index.png) |

## SQL-only Auxiliary Experiments

| Experiment | Path |
|---|---|
| Single-column index selectivity | [sql-only/single-status-index.txt](./sql-only/single-status-index.txt) |
| Composite index order | [sql-only/composite-order-index.txt](./sql-only/composite-order-index.txt) |
| Covering index | [sql-only/covering-index.txt](./sql-only/covering-index.txt) |
| Partial index | [sql-only/partial-index.txt](./sql-only/partial-index.txt) |

## Notes

- k6 성능 수치는 `k6-summary.json`을 기준 원본으로 사용한다.
- `k6-summary.txt`는 같은 wrapper 실행에서 생성된 사람이 읽는 보조 로그로 사용한다.
- Grafana screenshot은 `run-window.json`의 시간 범위에 맞춰 캡처한다.
- SQL-only evidence는 인덱스 planner 동작 설명용이며 API latency와 직접 비교하지 않는다.
- 모든 파일은 [runbook.md](../../phases/02-indexes/runbook.md)의 명령으로 재생성할 수 있어야 한다.
