# Phase 1. 나이브 구현과 베이스라인

Phase 1은 최적화 없는 나이브 구현을 실제 부하에서 측정해 Baseline을 확보하는 단계다. 이후 Phase 2, Phase 3, Phase 7에서 개선 전 기준값으로 사용한다.

## 현재 상태

Phase 1은 현재 evidence 기준으로 정리 완료 상태다.

- 주문 목록, 상품 검색, 포인트 내역 API 구현 완료
- N+1, 인덱스 없는 상품 필터 조회, no-index points page0 운영 한계 재현 완료
- k6 summary, `pg_stat_statements`, SQL EXPLAIN, DB 상태 snapshot 저장 완료
- Phase 2/3 비교 기준선 확보 완료
- Phase 7은 page0 운영 한계 기준선으로 연결하고, page0 vs page500 정밀 비교는 개선 후 재측정한다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 1 범위와 의도적 나이브 제약 |
| [runbook.md](./runbook.md) | Baseline 부하 테스트 실행 절차 |
| [observability.md](./observability.md) | 시나리오별 관측 지표와 해석 기준 |
| [report.md](./report.md) | Baseline 측정 결과와 다음 Phase 판단 |

## 구현된 API

| API | 의도한 병목 | 다음 비교 Phase |
|---|---|---|
| `GET /api/orders?userId=` | 반복적인 `order_item` 단건 조회로 N+1 재현 | Phase 3 |
| `GET /api/products?categoryId=&status=&strategy=baseline` | 인덱스 없는 상품 필터 조회 | Phase 2 |
| `GET /api/points?userId=&page=&size=` | page0부터 발생한 no-index pagination/count query 운영 한계 | Phase 7 |

## 관련 산출물

- 과거 구현 계획: [docs/superpowers/plans/phase-01-baseline/000-legacy-plan.md](../../superpowers/plans/phase-01-baseline/000-legacy-plan.md)
- Phase Evidence: [docs/evidence/phase-01/README.md](../../evidence/phase-01/README.md)
- 공통 k6 가이드: [docs/guides/k6-load-testing.md](../../guides/k6-load-testing.md)

## 다음 Phase 연결

- Phase 2: `products` 쿼리의 인덱스 전후 실행계획 비교
- Phase 3: `orders`의 N+1 제거 전후 비교
- Phase 7: `points` page0 운영 한계 회복과 이후 deep page 비교
