# Phase 3 Evidence

Phase 3 evidence는 주문 목록 API의 N+1 재현과 로딩 전략별 개선 결과를 보관한다.

## 측정 조건

| 항목 | 값 |
|---|---|
| phase | `phase-03` |
| scenario | `orders` |
| preset | `baseline` |
| pool | `pool10` |
| API | `GET /api/orders?userId=&strategy=` |
| baseUrl | `http://host.docker.internal:8080` |
| rate | `1` iteration/s |
| duration | `5m` |
| preAllocatedVUs | `20` |
| maxVUs | `100` |
| timeout | `30s` |
| user range | `1..1000` |
| page size | `20` |

## Strategy Evidence

| Strategy | k6 summary | pg_stat_statements | SQL count | representative SQL | EXPLAIN | Grafana |
|---|---|---|---|---|---|---|
| Lazy naive | [orders/lazy/k6-summary.txt](./orders/lazy/k6-summary.txt) | [orders/lazy/pg-stat-statements.txt](./orders/lazy/pg-stat-statements.txt) | [orders/lazy/sql-count.txt](./orders/lazy/sql-count.txt) | [orders/lazy/representative-sql.txt](./orders/lazy/representative-sql.txt) | [orders/lazy/explain.txt](./orders/lazy/explain.txt) | [orders/lazy/grafana-screenshot.png](./orders/lazy/grafana-screenshot.png) |
| Fetch Join | [orders/fetch-join/k6-summary.txt](./orders/fetch-join/k6-summary.txt) | [orders/fetch-join/pg-stat-statements.txt](./orders/fetch-join/pg-stat-statements.txt) | [orders/fetch-join/sql-count.txt](./orders/fetch-join/sql-count.txt) | [orders/fetch-join/representative-sql.txt](./orders/fetch-join/representative-sql.txt) | [orders/fetch-join/explain.txt](./orders/fetch-join/explain.txt) | [orders/fetch-join/grafana-screenshot.png](./orders/fetch-join/grafana-screenshot.png) |
| BatchSize | [orders/batch-size/k6-summary.txt](./orders/batch-size/k6-summary.txt) | [orders/batch-size/pg-stat-statements.txt](./orders/batch-size/pg-stat-statements.txt) | [orders/batch-size/sql-count.txt](./orders/batch-size/sql-count.txt) | [orders/batch-size/representative-sql.txt](./orders/batch-size/representative-sql.txt) | [orders/batch-size/explain.txt](./orders/batch-size/explain.txt) | [orders/batch-size/grafana-screenshot.png](./orders/batch-size/grafana-screenshot.png) |
| EntityGraph | [orders/entity-graph/k6-summary.txt](./orders/entity-graph/k6-summary.txt) | [orders/entity-graph/pg-stat-statements.txt](./orders/entity-graph/pg-stat-statements.txt) | [orders/entity-graph/sql-count.txt](./orders/entity-graph/sql-count.txt) | [orders/entity-graph/representative-sql.txt](./orders/entity-graph/representative-sql.txt) | [orders/entity-graph/explain.txt](./orders/entity-graph/explain.txt) | [orders/entity-graph/grafana-screenshot.png](./orders/entity-graph/grafana-screenshot.png) |

## Additional Lazy Collapse Evidence

| Evidence | Purpose |
|---|---|
| [orders/lazy/k6-summary-rate5.txt](./orders/lazy/k6-summary-rate5.txt) | FK index 적용 후에도 `rate=5`, `timeout=30s`에서 Lazy가 안정 상태에 도달하지 못한 capacity collapse 기록 |

## Capture Workflow

Use [docs/phases/03-n-plus-one/runbook.md](../../phases/03-n-plus-one/runbook.md) for the strategy evidence capture sequence. The SQL helpers live under `scripts/phase-03/`:

| Script | Purpose |
|---|---|
| `00-prepare-fk-indexes.sql` | Create baseline FK lookup indexes required for loading-strategy comparison |
| `00-reset-statistics.sql` | Reset `pg_stat_statements` and refresh table statistics before each strategy run |
| `01-pg-stat-statements.sql` | Capture top order-related SQL statements after k6 |
| `02-order-shape.sql` | Snapshot selected order-list shape for one `user_id` |
| `10-lazy-explain.sql` | Capture representative Lazy `EXPLAIN` |
| `20-fetch-join-explain.sql` | Capture representative Fetch Join `EXPLAIN` |
| `30-batch-size-explain.sql` | Capture representative BatchSize `EXPLAIN` |
| `40-entity-graph-explain.sql` | Capture representative EntityGraph `EXPLAIN` |

## Notes

- 각 전략 측정 전 `pg_stat_statements_reset()`을 실행한다.
- k6 실행은 `phase=phase-03`, `scenario=orders`, `preset=baseline`, `pool=pool10` 컨텍스트를 사용하고, 전략 구분은 `STRATEGY` 환경변수와 output 파일명으로 분리한다.
- `k6-summary.txt`는 실행 중 progress line이 많기 때문에 최종 지표 확인에는 파일 마지막 summary 영역을 사용한다.
- SQL 원문은 Prometheus label로 올리지 않고 evidence 파일로만 저장한다.
