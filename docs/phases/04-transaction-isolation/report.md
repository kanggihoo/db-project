# Phase 4 결과 보고서

## 결론

Phase 4는 Testcontainers PostgreSQL과 직접 제어한 JDBC connection 두 개를 사용해 격리 수준별 가시성 차이를 integration test로 재현했다.

PostgreSQL에서는 `READ UNCOMMITTED`를 요청해도 Dirty Read가 발생하지 않았다. `READ COMMITTED`에서는 Non-Repeatable Read와 Phantom Read가 재현됐고, `REPEATABLE READ`에서는 트랜잭션 시작 시점의 snapshot을 유지해 두 현상을 방지했다.

Lost Update는 naive read-modify-write 패턴에서 `READ COMMITTED`로 재현됐다. 같은 패턴을 `REPEATABLE READ`에서 실행하면 PostgreSQL이 SQLSTATE `40001` concurrent update failure로 stale write를 실패시켜 Lost Update가 조용히 발생하지 않았다.

## 격리 수준별 비교

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Lost Update / Concurrent Update | Notes |
|---|---|---|---|---|---|
| READ COMMITTED | 방지 | 발생 | 발생 | naive read-modify-write에서 발생 가능 | PostgreSQL 기본 격리 수준 |
| REPEATABLE READ | 방지 | 방지 | 방지 | SQLSTATE `40001` concurrent update failure로 방지 | PostgreSQL MVCC snapshot 확인 |
| SERIALIZABLE | 미측정 | 미측정 | 미측정 | 미측정 | 이번 Phase 4 구현 테스트 범위 밖의 문서 비교 대상 |

## Phase 5 Handoff

트랜잭션 격리 수준별 가시성과 동시 갱신 충돌 경계를 정리했다. Phase 5에서는 조회 레이어의 DTO projection과 동적 조건 조합을 다룬다. Atomic UPDATE, 비관적 락, 낙관적 락, retry, idempotency 전략 비교는 Phase 11로 넘긴다.
