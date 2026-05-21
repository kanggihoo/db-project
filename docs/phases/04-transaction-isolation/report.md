# Phase 4 결과 보고서

## 결론

Phase 4 측정 전이다. 이 문서는 READ COMMITTED, REPEATABLE READ, SERIALIZABLE evidence 수집 후 업데이트한다.

## 격리 수준별 비교

| Isolation Level | Non-Repeatable Read | Phantom Read | RPS | Failure Rate | Serialization Failure | Notes |
|---|---|---|---:|---:|---:|---|
| READ COMMITTED | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | PostgreSQL 기본 격리 수준 |
| REPEATABLE READ | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | PostgreSQL MVCC snapshot 확인 |
| SERIALIZABLE | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | retry 필요 여부 확인 |

## Phase 5 Handoff

트랜잭션 정합성 경계를 정리한 뒤, Phase 5에서는 조회 레이어의 DTO projection과 동적 조건 조합을 다룬다.
