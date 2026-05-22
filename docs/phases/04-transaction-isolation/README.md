# Phase 4. 트랜잭션 격리 수준

Phase 4는 동시에 여러 트랜잭션이 같은 Product/Product SKU 데이터를 읽거나 변경할 때 PostgreSQL 격리 수준에 따라 어떤 읽기 이상 현상과 동시 갱신 충돌 결과가 나타나는지 확인하는 Learning Phase다.

## 현재 상태

Phase 4는 완료됐다.

- Phase 3 N+1 로딩 전략 실험은 종료됐다.
- Phase 4는 조회 로딩 전략이 아니라 트랜잭션 격리 수준과 정합성 관찰을 다룬다.
- Dirty Read, Non-Repeatable Read, Phantom Read, Lost Update를 PostgreSQL 격리 수준별로 재현한다.
- 비관적 락/낙관적 락/Atomic UPDATE 전략 비교는 Phase 11 범위로 분리한다.
- Phase 4 시작 전 공통 k6 `baseline` preset은 50 rps 기준선으로 복원했고, Phase 3 재실행용 preset은 `phase3-orders-baseline`으로 분리했다. Phase 4의 기본 evidence는 k6가 아니라 실행 순서를 고정한 SQL transcript 또는 integration test output이다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 4 범위와 제외 범위 |
| [runbook.md](./runbook.md) | 반복 가능한 실행 절차 |
| [observability.md](./observability.md) | 격리 수준별 관측 기준 |
| [report.md](./report.md) | 격리 수준 측정 결과와 Phase 5 handoff |

## Source Documents

- Roadmap: [docs/roadmap/05-phase-4-transaction-isolation.md](../../roadmap/05-phase-4-transaction-isolation.md)
- Phase 3 handoff: [docs/phases/03-n-plus-one/report.md](../03-n-plus-one/report.md)

## 관련 산출물

- Phase Evidence: [docs/evidence/phase-04/README.md](../../evidence/phase-04/README.md)
- 공통 k6 가이드: [docs/guides/k6-load-testing.md](../../guides/k6-load-testing.md)
- 공통 Grafana 가이드: [docs/guides/grafana-observability.md](../../guides/grafana-observability.md)

## 다음 Phase 연결

Phase 4 이후 데이터 정합성 경계를 정리하면, Phase 5에서는 JPQL 한계를 넘는 QueryDSL 기반 동적 조회와 DTO projection 최적화로 넘어간다.
