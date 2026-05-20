# Phase 3. N+1 + 로딩 전략 최적화

Phase 3는 주문 목록 화면의 상품 썸네일 요구사항을 기준으로 `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` 조회 경로에서 N+1이 어떻게 발생하고, JPA 로딩 전략에 따라 SQL shape와 부하 지표가 어떻게 달라지는지 확인하는 Learning Phase다.

## 현재 상태

Phase 3는 준비 중이다.

- API는 `GET /api/orders?userId=&strategy=`를 사용한다.
- 비교 전략은 `lazy`, `fetch-join`, `batch-size`, `entity-graph`다.
- 핵심 evidence는 요청당 SQL 수, `pg_stat_statements`, k6, Grafana, 대표 EXPLAIN이다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 3 범위와 제외 범위 |
| [runbook.md](./runbook.md) | 반복 가능한 실행 절차 |
| [observability.md](./observability.md) | SQL count, k6, Grafana, EXPLAIN 해석 기준 |
| [report.md](./report.md) | 전략별 측정 결과와 Phase 4 handoff |

## Source Documents

- Roadmap: [docs/roadmap/04-phase-3-n-plus-one.md](../../roadmap/04-phase-3-n-plus-one.md)
- Spec: [docs/superpowers/specs/phase-3-n-plus-one-spec.md](../../superpowers/specs/phase-3-n-plus-one-spec.md)
- Plan: [docs/superpowers/plans/phase-03-n-plus-one/index.md](../../superpowers/plans/phase-03-n-plus-one/index.md)
- Phase 1 baseline: [docs/evidence/phase-01/BASELINE.md](../../evidence/phase-01/BASELINE.md)

## 관련 산출물

- Phase Evidence: [docs/evidence/phase-03/README.md](../../evidence/phase-03/README.md)
- 공통 k6 가이드: [docs/guides/k6-load-testing.md](../../guides/k6-load-testing.md)
- 공통 Grafana 가이드: [docs/guides/grafana-observability.md](../../guides/grafana-observability.md)

## 다음 Phase 연결

Phase 3 이후 조회 쿼리 수 문제가 정리되면, Phase 4에서는 동시에 여러 사용자가 주문/재고를 변경할 때의 데이터 정합성과 격리 수준 문제로 넘어간다.
