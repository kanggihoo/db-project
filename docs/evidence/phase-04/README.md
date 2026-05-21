# Phase 4 Evidence

Phase 4 evidence는 트랜잭션 격리 수준별 읽기 이상 현상과 동시 갱신 실패 비용을 보관한다.

## Expected Evidence

| Scenario | Path | Purpose |
|---|---|---|
| Read anomalies | [isolation-read-anomalies/](./isolation-read-anomalies/) | READ COMMITTED / REPEATABLE READ 조회 결과 transcript |
| Serializable conflicts | [serializable-conflicts/](./serializable-conflicts/) | SERIALIZABLE 동시 갱신 k6, SQL, Grafana evidence |
| Grafana screenshots | [grafana-screenshots/](./grafana-screenshots/) | Phase 4 dashboard screenshots |

## Expected Files Per Scenario

| File | Purpose |
|---|---|
| `k6-summary.txt` | k6 stdout/stderr summary |
| `pg-stat-statements.txt` | query shape별 calls, mean time, total time |
| `transaction-transcript.txt` | 격리 수준별 SQL 실행 순서와 조회 결과 |
| `server-log.txt` | serialization failure, rollback 원인 로그 |
| `grafana-screenshot.png` | Phase 4 focus screenshot |

## Notes

- Phase 4는 Phase 3의 orders loading strategy evidence와 분리한다.
- 공통 `baseline` preset은 50 rps 기준선이며, Phase 4 실험에는 별도 scenario/preset을 추가해서 사용한다.
