# Phase 3 관측 전략

Phase 3의 핵심 지표는 요청당 SQL 수와 query shape별 calls다. Grafana는 latency, failure, dropped iterations, Hikari active/pending/acquire time, PostgreSQL active sessions를 확인하는 보조 evidence로 사용한다.

## 핵심 Evidence

| Evidence | Purpose |
|---|---|
| `sql-count.txt` | 단일 요청 SQL 수 확인 |
| `pg-stat-statements.txt` | 부하 중 query shape별 calls와 total time 확인 |
| `k6-summary.txt` | p95/p99, failure, dropped iterations 확인 |
| `grafana-screenshot.png` | 커넥션 점유와 latency 흐름 확인 |
| `explain.txt` | 대표 SQL 하나의 실행계획 확인 |

## Fetch Join 해석

Fetch Join은 `Orders -> OrderItems -> ProductSku -> Product` 구간의 반복 select를 줄인다. `Product.images`는 두 번째 collection이므로 동시에 fetch join하면 row multiplication이 커질 수 있다. Phase 3 report는 fetch join SQL count와 row duplication 위험을 함께 기록한다.
