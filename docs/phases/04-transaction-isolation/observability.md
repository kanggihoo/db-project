# Phase 4 관측 전략

Phase 4의 핵심은 격리 수준별 읽기 결과 차이와 동시 갱신 실패 비용이다. Phase 3처럼 SQL count 자체가 핵심 지표는 아니며, 트랜잭션 결과와 실패율을 중심으로 본다.

## 핵심 Evidence

| Evidence | Purpose |
|---|---|
| SQL transcript | 같은 트랜잭션 안에서 조회 결과가 바뀌는지 기록 |
| `k6-summary.txt` | 격리 수준별 처리량, 실패율, p95 확인 |
| `pg-stat-statements.txt` | 격리 수준별 쿼리 실행시간 확인 |
| `grafana-screenshot.png` | failure, rollback, active sessions, Hikari pressure 확인 |
| server log excerpt | serialization failure 또는 transaction rollback 원인 확인 |

## Grafana 관측 포인트

| Panel | 해석 |
|---|---|
| Error Rate | SERIALIZABLE 충돌이 HTTP 실패로 이어지는지 |
| Actual RPS | 격리 수준 강화가 처리량을 낮추는지 |
| HTTP p95 by URI | 재시도 없는 실패 또는 대기 시간이 latency에 반영되는지 |
| Hikari Pending Threads | connection pool 대기가 병목인지 |
| PostgreSQL Rollback Rate | serialization failure 또는 rollback 증가 여부 |
| PostgreSQL Locks | 격리 수준별 lock 관측 변화 |

## 해석 기준

- READ COMMITTED에서 같은 트랜잭션 안의 두 조회 결과가 달라지면 Non-Repeatable Read로 기록한다.
- PostgreSQL REPEATABLE READ에서 Phantom Read가 방지되면 MVCC snapshot 특성으로 해석한다.
- SERIALIZABLE에서 serialization failure가 발생하면 정합성은 보장되지만 retry policy가 필요하다는 결론으로 연결한다.
