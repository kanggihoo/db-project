# DB Lab Grafana Dashboard Spec

> 공통 Grafana 대시보드가 각 Learning Phase의 Baseline, Pre-change Evidence, Post-change Evidence를 같은 기준으로 비교할 수 있게 하기 위한 요구사항이다.

## Goal

`DB Lab Overview` 대시보드 하나를 자동 프로비저닝하고, k6/Spring/Hikari/PostgreSQL 지표를 같은 시간축에서 확인한다.

이 대시보드는 Phase 1 baseline부터 Phase 2 indexes, Phase 3 N+1, Phase 7 pagination까지 반복해서 사용한다. Phase별로 별도 대시보드를 만들지 않고, 하나의 대시보드 안에서 공통 row와 Phase focus row를 둔다.

이 결정은 [ADR 0005](../adr/0005-use-one-observability-dashboard-for-comparable-phase-evidence.md)를 따른다. `phase`, `scenario`, `preset`, `pool`은 Phase Evidence의 Measurement Condition을 표현하는 낮은 cardinality label이다.

## Non-Goals

- Phase별 dashboard JSON을 따로 만들지 않는다.
- PostgreSQL query 원문을 Prometheus label로 수집하지 않는다.
- `userId`, `categoryId`, `page`, order id 같은 고카디널리티 값을 metric label로 붙이지 않는다.
- CPU/cAdvisor 기반 컨테이너 리소스 관측은 이번 범위에 포함하지 않는다.
- Grafana alert rule은 이번 범위에 포함하지 않는다.

## Decisions

### One Dashboard, Phase Rows

공통 대시보드 하나를 만든다.

```text
DB Lab Overview
  Run Summary
  k6 Load
  Spring API
  Hikari Pool
  PostgreSQL Activity
  Table Access
  Phase 1 Baseline Focus
  Phase 2 Index Focus
  Phase 3 N+1 Focus
  Phase 7 Pagination Focus
```

Phase별로 보는 원천 지표는 대부분 같다. 달라지는 것은 패널 자체가 아니라 해석 질문이다.

| Phase | Main Question | Reused Metrics |
|---|---|---|
| Phase 1 | 나이브 구현의 기준선은 얼마인가? | k6 latency, errors, Hikari, table scans |
| Phase 2 | 인덱스 후 Seq Scan이 Index Scan으로 바뀌는가? | table seq/index scan, tuples read/fetched |
| Phase 3 | N+1 제거 후 커넥션 점유와 latency가 줄어드는가? | k6 latency, Hikari active/pending, pg_stat_statements snapshot |
| Phase 7 | Offset deep page가 얕은 page보다 느린가? | k6 latency by preset, table scan, Hikari active |

### Measurement Condition Labels

k6 metric에는 낮은 cardinality의 실험 조건만 label로 붙인다.

| Label | Example | Purpose |
|---|---|---|
| `phase` | `phase-01` | Learning Phase 구분 |
| `scenario` | `orders`, `products`, `points` | Target API 구분 |
| `preset` | `baseline`, `stress-100`, `points-page500` | 부하/데이터 접근 조건 구분 |
| `pool` | `pool5`, `pool10`, `pool20` | Spring Hikari profile 비교 |

`page` 값은 별도 label로 만들지 않고 `points-page0`, `points-page500`처럼 preset 이름으로 표현한다.

### Metric Sources

| Source | Path | Transport |
|---|---|---|
| k6 | `k6/*.js` | `experimental-prometheus-rw` remote write |
| Spring Boot | `/actuator/prometheus` | Prometheus scrape |
| HikariCP | Micrometer Hikari metrics | Prometheus scrape |
| PostgreSQL | `postgres-exporter` | Prometheus scrape |
| Query-level SQL | `pg_stat_statements` SQL snapshot | Evidence document, not Prometheus label |

k6 remote write metric mapping follows Grafana k6 Prometheus remote write behavior: k6 metrics are exported with the `k6_` prefix, counters use `_total`, rates use `_rate`, and trend metrics can be sent as native histograms when `K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true`.

## Dashboard Variables

| Variable | Query / Values | Default |
|---|---|---|
| `$phase` | `label_values(k6_http_reqs_total, phase)` | `phase-01` |
| `$scenario` | `label_values(k6_http_reqs_total{phase="$phase"}, scenario)` | `orders` |
| `$preset` | `label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario"}, preset)` | `baseline` |
| `$pool` | `label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario", preset="$preset"}, pool)` | `pool10` |
| `$uri` | `label_values(http_server_requests_seconds_count, uri)` | All |
| `$table` | `label_values(pg_stat_user_tables_seq_scan, relname)` | All |

If a metric name changes because of Spring Boot, Micrometer, k6, or postgres-exporter version changes, implementation must first verify available metric names through Prometheus before adjusting panel queries.

## Rows And Panels

### Run Summary

Representative values for `BASELINE.md` and Phase Evidence.

| Panel | Type | Query Shape | Reducer |
|---|---|---|---|
| k6 p95 | Stat | `histogram_quantile(0.95, sum(rate(k6_http_req_duration_seconds{...}[$__rate_interval])))` | Last non-null |
| k6 p99 | Stat | `histogram_quantile(0.99, sum(rate(k6_http_req_duration_seconds{...}[$__rate_interval])))` | Last non-null |
| Error Rate | Stat | `avg(k6_http_req_failed_rate{...})` | Max |
| Actual RPS | Stat | `sum(rate(k6_http_reqs_total{...}[$__rate_interval]))` | Mean |
| Dropped Iterations | Stat | `sum(increase(k6_dropped_iterations_total{...}[$__range]))` | Last non-null |
| Hikari Pending Max | Stat | `max_over_time(hikaricp_connections_pending[$__range])` | Last non-null |

### k6 Load

부하가 의도대로 들어갔는지, latency spike가 언제 생겼는지 확인한다.

| Panel | Type | Query Shape |
|---|---|---|
| Actual RPS | Time series | `sum(rate(k6_http_reqs_total{...}[$__rate_interval]))` |
| k6 Latency p50/p95/p99 | Time series | `histogram_quantile(...)` on `k6_http_req_duration_seconds` |
| Error Rate | Time series | `avg(k6_http_req_failed_rate{...})` |
| Dropped Iterations | Time series | `sum(rate(k6_dropped_iterations_total{...}[$__rate_interval]))` |

### Spring API

앱 관점에서 URI별 처리량, latency, error status를 본다.

| Panel | Type | Query Shape |
|---|---|---|
| HTTP Request Rate by URI | Time series | `sum by (uri) (rate(http_server_requests_seconds_count{uri=~"$uri"}[$__rate_interval]))` |
| HTTP p95 by URI | Time series | `histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{uri=~"$uri"}[$__rate_interval])))` |
| HTTP Errors by Status | Time series | `sum by (status) (rate(http_server_requests_seconds_count{status!~"2.."}[$__rate_interval]))` |
| Slowest URI | Table | URI별 p95 descending |

### Hikari Pool

커넥션 풀이 병목인지 판단한다.

| Panel | Type | Query Shape |
|---|---|---|
| Active vs Max Connections | Time series | `hikaricp_connections_active`, `hikaricp_connections_max` |
| Pending Threads | Time series | `hikaricp_connections_pending` |
| Idle Connections | Time series | `hikaricp_connections_idle` |
| Acquire Time | Time series | average from `hikaricp_connections_acquire_seconds_sum/count`; p95 only if bucket metric exists |

### PostgreSQL Activity

DB 처리 상태와 잠금 문제를 확인한다.

| Panel | Type | Query Shape |
|---|---|---|
| Active Sessions | Time series | `sum(pg_stat_activity_count{state="active"})` |
| Locks | Time series | `sum by (mode) (pg_locks_count)` |
| Deadlocks | Stat + Time series | `sum(increase(pg_stat_database_deadlocks[$__range]))` |
| Commit / Rollback Rate | Time series | `rate(pg_stat_database_xact_commit[$__rate_interval])`, `rate(pg_stat_database_xact_rollback[$__rate_interval])` |
| Cache Hit Ratio | Stat + Time series | `blks_hit / (blks_hit + blks_read)` |

### Table Access

Phase 2 indexes와 Phase 7 pagination에서 핵심 증거가 된다.

| Panel | Type | Query Shape |
|---|---|---|
| Seq Scan by Table | Time series | `sum by (relname) (rate(pg_stat_user_tables_seq_scan{relname=~"$table"}[$__rate_interval]))` |
| Index Scan by Table | Time series | `sum by (relname) (rate(pg_stat_user_tables_idx_scan{relname=~"$table"}[$__rate_interval]))` |
| Seq Tuples Read Top N | Table | `topk(10, increase(pg_stat_user_tables_seq_tup_read[$__range]))` |
| Index Tuples Fetch Top N | Table | `topk(10, increase(pg_stat_user_tables_idx_tup_fetch[$__range]))` |
| Table Size | Table | `pg_stat_user_tables_table_size_bytes` |

## Evidence Workflow

1. Start infrastructure with `docker compose up -d`.
2. Start Spring server with a pool profile, for example `./scripts/server.sh pool10`.
3. Reset DB statistics before a scenario.
4. Run k6 in Prometheus mode with labels:

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
```

5. In Grafana, select matching `$phase`, `$scenario`, `$preset`, `$pool`.
6. Capture Run Summary and relevant focus row screenshots.
7. Save `pg_stat_statements` snapshot separately under `docs/evidence/`.

## Acceptance Criteria

- Grafana provisions `DB Lab Overview` from `docker/grafana/dashboards/db-lab-overview.json`.
- Grafana datasource provisioning gives Prometheus a stable UID.
- k6 metrics include `phase`, `scenario`, `preset`, and `pool` labels.
- k6 metrics do not use high-cardinality request identifiers as labels.
- The dashboard has rows for Run Summary, k6 Load, Spring API, Hikari Pool, PostgreSQL Activity, Table Access, and Phase focus sections.
- The dashboard can show values after running at least one smoke scenario in Prometheus mode.
- Documentation explains how to run k6 with labels and how to use the dashboard for Phase Evidence.
