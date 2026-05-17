# Grafana Observability Guide

> Phase 전반에서 반복해서 보는 공통 Grafana, PostgreSQL, JVM 지표를 정리한다.

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
| PostgreSQL | CPU / IO | DB 자원 병목 여부 |
| PostgreSQL | seq scan count | 인덱스 없는 스캔 증가 |
| pg_stat_statements | calls, mean_exec_time, total_exec_time, rows | 쿼리별 비용 |
| JVM | heap, GC pause | 앱 자체 병목 여부 |

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
| pending = 0, p95 증가 | pool보다 query, DB CPU, IO 병목 가능성 |
| pool20에서 p95 개선 | pool 확장이 병목 완화에 일부 효과 |
| pool20에서도 개선 없음 | query/DB 병목 가능성 |
| pool20에서 더 나빠짐 | DB 동시 실행 경합 증가 |

