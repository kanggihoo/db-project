# Grafana Observability Guide

> Phase 전반에서 반복해서 보는 공통 Grafana, PostgreSQL, JVM 지표를 정리한다.

## DB Lab Overview

공통 대시보드는 `DB Lab Overview` 하나를 기준으로 한다. Phase별로 dashboard JSON을 나누지 않고, 공통 row와 Phase focus row를 함께 둔다.

상세 요구사항은 [DB Lab Grafana Dashboard Spec](./db-lab-grafana-dashboard-spec.md)을 기준으로 한다. 이 결정의 배경은 [ADR 0005](../adr/0005-use-one-observability-dashboard-for-comparable-phase-evidence.md)에 기록한다.

## Measurement Conditions

k6 지표는 비교 가능한 Phase Evidence를 위해 낮은 cardinality label만 사용한다.

| Label | Example | Meaning |
|---|---|---|
| `phase` | `phase-01` | Learning Phase |
| `scenario` | `orders` | k6 scenario |
| `preset` | `baseline` | workload preset |
| `pool` | `pool10` | Spring Hikari profile |

`userId`, `categoryId`, `page`, 주문 ID, SQL 원문처럼 값 종류가 많은 항목은 Prometheus label로 수집하지 않는다. `page` 비교는 `points-page0`, `points-page500`처럼 preset으로 표현한다.

## Common Metrics

| Area | Metric | Why |
|---|---|---|
| k6 | `http_req_duration p50/p95/p99` | 사용자 체감 지연 |
| k6 | `http_req_failed` | 실패율, 타임아웃 |
| k6 | `http_reqs rate` | 실제 처리량 |
| Spring MVC | HTTP server request p95 | 애플리케이션 기준 응답시간 |
| HikariCP | active connections | 커넥션 풀 사용량 |
| HikariCP | pending threads | 커넥션 대기 발생 여부 |
| HikariCP | connection acquire time | 커넥션 획득 지연 |
| PostgreSQL | active sessions | DB가 실제 처리 중인 세션 수 |
| PostgreSQL | seq scan count | 인덱스 없는 스캔 증가 |
| PostgreSQL | index scan count | 인덱스 사용 증가 |
| PostgreSQL | tuples read/fetched | 스캔 비용과 인덱스 전환 확인 |
| PostgreSQL | locks/deadlocks | 트랜잭션 경합 여부 |
| pg_stat_statements | calls, mean_exec_time, total_exec_time, rows | 쿼리별 비용 |
| JVM | heap, GC pause | 앱 자체 병목 여부 |

CPU와 컨테이너 IO는 기본 대시보드 범위가 아니다. 현재 구성의 postgres-exporter만으로 PostgreSQL 컨테이너 CPU를 직접 대표하지 않기 때문에, DB 병목 판단은 table access, active sessions, locks, Hikari pending, `pg_stat_statements`를 우선한다.

## Suggested Panels

| Panel | Prometheus example |
|---|---|
| HTTP p95 by URI | `histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket[1m])))` |
| HTTP request rate by URI | `sum by (uri) (rate(http_server_requests_seconds_count[1m]))` |
| Hikari active connections | `hikaricp_connections_active` |
| Hikari pending threads | `hikaricp_connections_pending` |
| Hikari max connections | `hikaricp_connections_max` |
| JVM GC pause p95 | `histogram_quantile(0.95, sum by (le) (rate(jvm_gc_pause_seconds_bucket[1m])))` |
| PostgreSQL active sessions | postgres-exporter의 activity 관련 metric |
| PostgreSQL seq scan | postgres-exporter의 table scan 관련 metric |
| PostgreSQL index scan | postgres-exporter의 index scan 관련 metric |
| PostgreSQL locks | postgres-exporter의 lock 관련 metric |

Metric 이름은 Spring Boot, Micrometer, postgres-exporter 버전에 따라 다를 수 있다. Grafana 자동완성에 나오는 실제 metric 이름을 기준으로 패널을 만든다.

## pg_stat_statements Snapshot

```bash
docker compose exec postgres psql -U app -d ecommerce -c "
SELECT calls,
       round(mean_exec_time::numeric, 2) AS mean_ms,
       round(total_exec_time::numeric, 2) AS total_ms,
       rows,
       left(query, 180) AS query
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
"
```

## Hikari Pool Interpretation

| Observation | Meaning |
|---|---|
| pending > 0, active near max | 커넥션 풀 대기 병목 |
| pending = 0, p95 증가 | pool보다 query shape, table scan, lock, or data volume 병목 가능성 |
| pool20에서 p95 개선 | pool 확장이 병목 완화에 일부 효과 |
| pool20에서도 개선 없음 | query/DB 병목 가능성 |
| pool20에서 더 나빠짐 | DB 동시 실행 경합 증가 |
