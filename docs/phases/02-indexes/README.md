# Phase 2. 인덱스 설계와 실행계획 분석

Phase 2는 Phase 1 상품 검색 Baseline을 기준으로 `product(category_id, status)` 필터 쿼리에 인덱스를 적용하고, 실행계획과 성능 지표가 어떻게 바뀌는지 확인하는 Learning Phase다.

## 현재 상태

준비 중

## 실험 기준

- 비교 기준은 Phase 1의 `products/pool10-baseline` 결과다.
- 메인 실험은 기존 `GET /api/products?categoryId=&status=` API를 그대로 사용한다.
- 변경 대상은 PostgreSQL 인덱스와 Evidence이며, API/Controller/Service/Repository 흐름은 성능 개선 목적으로 바꾸지 않는다.
- SQL-only 보조 실험은 API를 통하지 않고 `psql`과 `EXPLAIN (ANALYZE, BUFFERS)`로 별도 실행한다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 2 목표, 대상 API/table, 제외 범위, 완료 조건 |
| [runbook.md](./runbook.md) | 인덱스 적용 전후 Evidence 수집 절차 |
| [observability.md](./observability.md) | EXPLAIN, `pg_stat_statements`, k6, Grafana 해석 기준 |
| [report.md](./report.md) | 측정 결과, SQL-only 보조 실험 해석, Phase 3 handoff |

## 원천 문서

- [Phase 2 roadmap](../../roadmap/03-phase-2-indexes.md)
- [Phase 2 Index Optimization Spec](../../superpowers/specs/phase-2-index-optimization-spec.md)
- [Phase 2 Index Optimization Plan](../../superpowers/plans/phase-2-index-optimization/000-plan-index.md)
- [Phase 1 Baseline Evidence](../../evidence/phase-01/BASELINE.md)

## 관련 산출물

- [Phase 2 Evidence](../../evidence/phase-02/README.md)
- [k6 Load Testing Guide](../../guides/k6-load-testing.md)
- [Grafana Observability Guide](../../guides/grafana-observability.md)

## Phase 3 연결

Phase 2에서 상품 검색 인덱스 적용 후에도 남는 반복 SQL, query count, 주문 목록 병목은 Phase 3의 N+1 최적화 대상으로 넘긴다.
