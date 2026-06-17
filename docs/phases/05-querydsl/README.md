# Phase 5. QueryDSL 상품 검색

Phase 5는 현재 Product 엔티티 기반 상품 검색 baseline과 QueryDSL DTO projection을 같은 상품 검색 API에서 비교한다.

## 현재 상태

Phase 5는 완료되었다.

- API 계약: `GET /api/products`
- 전략 파라미터: `strategy=baseline|querydsl`
- 기본 동작: `strategy` 생략 시 `querydsl` 사용
- `baseline`: Spring Data JPA 엔티티 조회 후 `ProductResponse.from(product)` 변환
- `querydsl`: `ProductResponse`로 QueryDSL DTO projection
- 상품 검색 증거는 대표 SQL shape와 focused integration test 출력을 기록한다.
- Bulk update 증거는 SQL count와 persistence context 동작을 기록한다.
- k6와 Grafana는 선택 참고 자료이며 Phase 5 closeout 필수 증거가 아니다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 5 범위, 제외 범위, 완료 조건 |
| [runbook.md](./runbook.md) | focused test와 evidence capture 반복 실행 절차 |
| [observability.md](./observability.md) | SQL, Hibernate statistics, 선택 관측 항목 |
| [report.md](./report.md) | Phase 5 최종 결과와 Phase 6 handoff |

## 원본 문서

- Roadmap: [docs/roadmap/06-phase-5-querydsl.md](../../roadmap/06-phase-5-querydsl.md)
- Design spec: [docs/superpowers/specs/2026-05-26-phase-5-querydsl-design.md](../../superpowers/specs/2026-05-26-phase-5-querydsl-design.md)
- Phase 4 report: [docs/phases/04-transaction-isolation/report.md](../04-transaction-isolation/report.md)

## 증거

- Phase 5 evidence index: [docs/evidence/phase-05/README.md](../../evidence/phase-05/README.md)
