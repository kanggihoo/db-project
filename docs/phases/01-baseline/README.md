# Phase 1. 나이브 구현과 베이스라인

Phase 1은 최적화 없는 나이브 구현을 실제 부하에서 측정해 Baseline을 확보하는 단계다. 이후 Phase 2, Phase 3, Phase 7에서 개선 전 기준값으로 사용한다.

## 현재 상태

Phase 1은 완료된 상태다.

- 주문 목록, 상품 검색, 포인트 내역 API 구현 완료
- N+1, 인덱스 없는 필터 조회, Offset pagination 병목 재현 완료
- k6 summary, `pg_stat_statements`, Grafana screenshot 저장 완료
- Phase 2 진입 가능 판단 완료

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 1 범위와 의도적 나이브 제약 |
| [runbook.md](./runbook.md) | Baseline 부하 테스트 실행 절차 |
| [observability.md](./observability.md) | 시나리오별 관측 지표와 해석 기준 |
| [report.md](./report.md) | Baseline 측정 결과와 다음 Phase 판단 |
| [report.html](./report.html) | Baseline 보고서 보기용 HTML |

## 구현된 API

| API | 의도한 병목 | 다음 비교 Phase |
|---|---|---|
| `GET /api/orders?userId=` | 반복적인 `order_item` 단건 조회로 N+1 재현 | Phase 3 |
| `GET /api/products?categoryId=&status=` | 인덱스 없는 상품 필터 조회 | Phase 2 |
| `GET /api/points?userId=&page=&size=` | Offset pagination과 count query 비용 | Phase 7 |

## 관련 산출물

- 과거 구현 계획: [docs/superpowers/plans/phase-01-baseline/000-legacy-plan.md](../../superpowers/plans/phase-01-baseline/000-legacy-plan.md)
- Phase Evidence: [docs/evidence/phase-01/BASELINE.md](../../evidence/phase-01/BASELINE.md)
- 공통 k6 가이드: [docs/guides/k6-load-testing.md](../../guides/k6-load-testing.md)
- 공통 Grafana 가이드: [docs/guides/grafana-observability.md](../../guides/grafana-observability.md)

## 다음 Phase 연결

- Phase 2: `products` 쿼리의 인덱스 전후 실행계획 비교
- Phase 3: `orders`의 N+1 제거 전후 비교
- Phase 7: `points`의 Offset pagination 제거 전후 비교
