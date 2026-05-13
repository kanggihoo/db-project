# 이커머스 DB 최적화 학습 로드맵

> **핵심 철학:** "최적화 없는 나이브한 구현"에서 시작하여, 각 Phase에서 발생하는 문제를 수치로 직접 확인하고, 해결책을 하나씩 도입한다.
> 단순히 "개선했다"가 아니라 **k6 + Grafana + PostgreSQL 관측 지표로 Before/After를 정량적으로 증명**하는 것이 목표다.

이 문서는 전체 로드맵의 인덱스입니다. 상세 내용은 단계별 문서를 기준으로 관리합니다.

## 단계별 문서

| 순서 | 문서 | 핵심 주제 |
|---:|---|---|
| 00 | [Overview](docs/roadmap/00-overview.md) | 기술 스택, ERD, 전체 흐름, 학습 질문 |
| 01 | [Phase 0. 프로젝트 초기 셋팅](docs/roadmap/01-phase-0-setup.md) | Docker Compose, 스키마, 더미 데이터 |
| 02 | [Phase 1. 나이브한 구현 + 베이스라인 확보](docs/roadmap/02-phase-1-baseline.md) | k6 기준선, 풀스캔, N+1, Offset 병목 |
| 03 | [Phase 2. 인덱스 설계 + 실행계획 분석](docs/roadmap/03-phase-2-indexes.md) | EXPLAIN ANALYZE, 복합/커버링/부분 인덱스 |
| 04 | [Phase 3. N+1 + 로딩 전략 최적화](docs/roadmap/04-phase-3-n-plus-one.md) | Fetch Join, EntityGraph, BatchSize |
| 05 | [Phase 4. 트랜잭션 격리 수준](docs/roadmap/05-phase-4-transaction-isolation.md) | READ COMMITTED, REPEATABLE READ, SERIALIZABLE |
| 06 | [Phase 5. 쿼리 최적화 + QueryDSL](docs/roadmap/06-phase-5-querydsl.md) | DTO Projection, 동적 쿼리, 벌크 연산 |
| 07 | [Phase 6. 집계 쿼리 최적화](docs/roadmap/07-phase-6-aggregation.md) | GROUP BY, 표현식 인덱스, 집계 실행계획 |
| 08 | [Phase 7. 페이지네이션 최적화](docs/roadmap/08-phase-7-pagination.md) | Offset vs Cursor, Count 쿼리 분리 |
| 09 | [Phase 8. DB Observability](docs/roadmap/09-phase-8-db-observability.md) | pg_stat_statements, pg_stat_activity, pg_locks, alert |
| 10 | [Phase 9. Failure Injection](docs/roadmap/10-phase-9-failure-injection.md) | 커넥션 고갈, Lock wait, Deadlock, DB restart, runbook |
| 11 | [Phase 10. Production Schema Migration](docs/roadmap/11-phase-10-production-schema-migration.md) | Flyway, expand-contract, backfill, validation, rollback |
| 12 | [Phase 11. Concurrency Control](docs/roadmap/12-phase-11-concurrency-control.md) | 재고 차감, 쿠폰 발급, 락, retry, idempotency |
| 13 | [Phase 12. Outbox Pattern](docs/roadmap/13-phase-12-outbox-pattern.md) | 주문/결제 이벤트 저장, polling publisher, 장애 격리, 재발행 |
| 14 | [Phase 13. CDC + Kafka](docs/roadmap/14-phase-13-cdc-kafka.md) | Outbox CDC, PostgreSQL WAL, Debezium, Kafka replay, lag 복구 |

## 운영 원칙

- 복잡한 ERD는 유지하되, 각 Phase에서는 대표 테이블만 좁혀서 실험한다.
- 각 Phase는 `문제 재현 → 측정 → 개선 → 재측정 → 문서화` 순서로 진행한다.
- k6는 Phase 1부터 사용하되, 먼저 테스트와 SQL 분석으로 원인을 확인한 뒤 운영 부하에서 p95/TPS/에러율을 측정한다.
- 장애 주입은 SQL 스크립트, k6, Docker 명령으로 시작하고, 반복 실험이 필요할 때 `lab` 프로필 전용 API를 추가한다.
- 새 주제를 추가할 때는 `docs/roadmap/NN-phase-name.md` 형식으로 별도 파일을 만든다.
- Phase 전환은 해당 Phase 문서의 완료 조건과 증빙 자료가 채워졌을 때만 진행한다.
- 결과 수치는 가능하면 `BASELINE.md`, `PHASE*_RESULT.md`, `docs/evidence/`에 분리해 기록한다.
