# Phase 3 Evidence

Phase 3 evidence는 주문 목록 API의 N+1 재현과 로딩 전략별 개선 결과를 보관한다.

## Strategy Evidence

| Strategy | Path |
|---|---|
| Lazy naive | [orders/lazy/](./orders/lazy/) |
| Fetch Join | [orders/fetch-join/](./orders/fetch-join/) |
| BatchSize | [orders/batch-size/](./orders/batch-size/) |
| EntityGraph | [orders/entity-graph/](./orders/entity-graph/) |

## Expected Files Per Strategy

| File | Purpose |
|---|---|
| `k6-summary.txt` | k6 stdout/stderr summary |
| `pg-stat-statements.txt` | query shape별 calls, mean time, total time |
| `sql-count.txt` | 단일 요청 SQL count |
| `representative-sql.txt` | 전략별 대표 SQL 원문 |
| `explain.txt` | 대표 SQL의 `EXPLAIN (ANALYZE, BUFFERS)` |
| `grafana-screenshot.png` | Phase 3 focus screenshot |

## Notes

- 각 전략 측정 전 `pg_stat_statements_reset()`을 실행한다.
- k6 label은 `phase=phase-03`, `scenario=orders`, `preset=<preset>`, `pool=<pool>`, `strategy=<strategy>`를 사용한다.
- SQL 원문은 Prometheus label로 올리지 않고 evidence 파일로만 저장한다.
