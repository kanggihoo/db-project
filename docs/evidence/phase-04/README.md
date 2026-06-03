# Phase 4 Evidence

Phase 4 evidence는 트랜잭션 격리 수준별 읽기 이상 현상과 동시 갱신 충돌 결과를 보관한다.

## Expected Evidence

| Scenario | Path | Purpose |
|---|---|---|
| Dirty Read | [dirty-read/](./dirty-read/) | PostgreSQL에서 커밋 전 변경값이 보이지 않는지 확인 |
| Non-Repeatable Read | [non-repeatable-read/](./non-repeatable-read/) | READ COMMITTED / REPEATABLE READ 조회 결과 transcript |
| Phantom Read | [phantom-read/](./phantom-read/) | PostgreSQL REPEATABLE READ의 Phantom Read 방지 확인 |
| Lost Update | [lost-update/](./lost-update/) | 격리 수준별 naive read-modify-write 동시 갱신 결과 확인 |
| Optional Load | [optional-load/](./optional-load/) | 필요 시 k6, SQL, Grafana evidence |

## Required Files Per Scenario

| File | Purpose |
|---|---|
| `integration-test-output.txt` | 두 thread 또는 두 connection 기반 재현 테스트 결과 |

## Optional Files Per Scenario

| File | Purpose |
|---|---|
| `transaction-transcript.txt` | 필요 시 격리 수준별 SQL 실행 순서와 조회 결과 |
| `server-log.txt` | concurrent update failure, serialization failure, rollback 원인 로그 |
| `pg-stat-statements.txt` | 선택 evidence. query shape별 calls, mean time, total time |
| `k6-summary.txt` | 선택 evidence. k6 stdout/stderr summary |
| `grafana-screenshot.png` | 선택 evidence. Phase 4 focus screenshot |

## Notes

- Phase 4는 Phase 3의 orders loading strategy evidence와 분리한다.
- Phase 4의 기본 evidence는 실행 순서를 고정한 SQL transcript 또는 integration test output이다.
- 공통 `baseline` preset은 50 rps 기준선이며, Phase 4에서 k6를 사용할 경우 별도 scenario/preset을 추가해서 사용한다.
