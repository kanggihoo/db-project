# Phase 4 관측 전략

Phase 4의 핵심은 격리 수준별 읽기 결과 차이와 동시 갱신 충돌 결과다. Phase 3처럼 SQL count나 RPS가 핵심 지표는 아니며, 트랜잭션 실행 순서와 결과를 중심으로 본다.

## 핵심 Evidence

| Evidence | Purpose |
|---|---|
| integration test output | 두 thread 또는 두 connection으로 고정한 재현 결과 기록 |
| SQL transcript | 선택 evidence. 같은 트랜잭션 안에서 조회 결과가 바뀌는지 기록 |
| server log excerpt | 선택 evidence. concurrent update failure, transaction rollback 원인 확인 |
| `pg-stat-statements.txt` | 필요 시 격리 수준별 query snapshot 확인 |
| `k6-summary.txt` | 선택 evidence. 반복 부하가 필요한 경우 처리량, 실패율, p95 확인 |
| `grafana-screenshot.png` | 선택 evidence. failure, rollback, active sessions, Hikari pressure 확인 |

## Grafana 관측 포인트

Grafana는 Phase 4의 필수 evidence가 아니다. thread 기반 재현 테스트 이후 반복 부하나 runtime pressure를 추가로 확인할 때만 사용한다.

| Panel | 해석 |
|---|---|
| Error Rate | 동시 갱신 충돌이 HTTP 실패로 이어지는지 |
| Actual RPS | 격리 수준 강화가 처리량을 낮추는지 |
| HTTP p95 by URI | 재시도 없는 실패 또는 대기 시간이 latency에 반영되는지 |
| Hikari Pending Threads | connection pool 대기가 병목인지 |
| PostgreSQL Rollback Rate | concurrent update failure 또는 rollback 증가 여부 |
| PostgreSQL Locks | 격리 수준별 lock 관측 변화 |

## 해석 기준

- PostgreSQL에서 Dirty Read가 발생하지 않으면 커밋 전 변경값을 읽지 않는 것으로 기록한다.
- READ COMMITTED에서 같은 트랜잭션 안의 두 조회 결과가 달라지면 Non-Repeatable Read로 기록한다.
- PostgreSQL REPEATABLE READ에서 Phantom Read가 방지되면 MVCC snapshot 특성으로 해석한다.
- READ COMMITTED의 naive read-modify-write에서 stale value 덮어쓰기가 가능하면 Lost Update 위험으로 기록한다.
- PostgreSQL REPEATABLE READ에서 같은 row 동시 갱신이 concurrent update failure로 실패하면 Lost Update가 조용히 발생하지 않는 것으로 기록한다.
- Atomic UPDATE, 비관적 락, 낙관적 락, retry policy 비교는 Phase 11로 넘긴다.
