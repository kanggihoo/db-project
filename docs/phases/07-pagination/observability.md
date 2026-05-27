# Phase 7 Observability

## 주요 증거

| 증거 | 역할 |
|---|---|
| `EXPLAIN (ANALYZE, BUFFERS)` | Offset deep과 Cursor deep의 DB 실행계획 비교 |
| k6 summary | API p95 비교 |
| `pg_stat_statements` | data query와 count query 호출/시간 비교 |
| Grafana screenshot | 같은 시간대의 HTTP, Hikari, table access 보조 확인 |

## Grafana 사용

별도 Phase 7 dashboard를 만들지 않고 shared `DB Lab Overview`를 사용한다.

캡처 대상:

- Run Summary
- k6 Load
- Hikari Pool
- Table Access에서 `point_history`

Grafana는 보조 evidence다. Offset이 읽고 지나간 row 수 차이는 `EXPLAIN (ANALYZE, BUFFERS)`로 판단한다.

## k6 label 원칙

- `phase`: `phase-07`
- `scenario`: `points-offset`, `points-cursor`
- `preset`: `page0`, `mid`, `deep`, `cursor`
- `pool`: `pool10`

`userId`, `page`, SQL text는 label로 넣지 않는다.
