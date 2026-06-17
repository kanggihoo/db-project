# Phase 2 범위

## 목표

Phase 2의 목표는 Phase 1에서 남긴 product baseline 원본 evidence와 비교 가능한 인덱스 전후 evidence를 남기고, PostgreSQL 실행계획을 근거로 인덱스 설계 판단을 설명하는 것이다.

Phase 1의 전체 결론은 points 조회가 page0부터 운영 한계에 도달했다는 것이다. Phase 2는 그 결론을 바꾸지 않는다. 여기서는 Phase 1 evidence 중 `products/products-baseline`만 상품 검색 API 비교 기준으로 사용한다.

- Phase 1 `products/products-baseline` 원본 evidence를 비교 기준으로 사용한다.
- 인덱스 적용 전 상품 검색 SQL의 `EXPLAIN`과 `EXPLAIN (ANALYZE, BUFFERS)`를 기록한다.
- `idx_product_category_status ON product(category_id, status)`를 적용한다.
- 인덱스 적용 후 `EXPLAIN`, k6, `pg_stat_statements`, Grafana screenshot을 수집한다.
- 단일 컬럼 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스는 SQL-only 보조 실험으로 분리한다.

## 대상 API와 테이블

| 항목 | 값 |
|---|---|
| API | `GET /api/products?categoryId=&status=&strategy=baseline` |
| Repository | `ProductRepository.findByCategoryIdAndStatus` |
| Table | `product` |
| Main query shape | `product where category_id = ? and status = ?` |
| Main index | `idx_product_category_status ON product(category_id, status)` |
| k6 scenario | `products` |
| k6 preset | `baseline` |
| k6 condition | `pool10-post-index` |
| Spring profile | `pool10` |
| Seed preset | `loadtest` |

## Phase 1 비교 기준

아래 값은 Phase 1 report의 문장이나 과거 Phase 2 문서가 아니라, `docs/evidence/phase-01/products/products-baseline/` 원본 evidence에서 다시 읽은 값이다.

| 항목 | 값 |
|---|---:|
| k6 scenario | `products` |
| preset | `baseline` |
| pool | `pool10` |
| request rate | 49.78 rps |
| requests | 14,935 |
| API p95 | 621.91ms |
| API p99 | 2065.98ms |
| failed rate | 0.0134% |
| dropped iterations | 66 |
| SQL calls | 14,935 |
| SQL mean time | 21.45ms |
| SQL total time | 320,401.33ms |

## 제외 범위

- API/Controller/Service/Repository 성능 개선은 하지 않는다.
- DTO projection, QueryDSL, fetch join, batch loading은 적용하지 않는다.
- Flyway/Liquibase migration은 도입하지 않는다.
- SQL-only 보조 실험 결과를 API/k6 latency와 직접 비교하지 않는다.
- Phase 1 Grafana screenshot이 없으므로 Grafana 전후 정량 비교는 하지 않는다.
- 주문 목록 N+1과 pagination 병목은 Phase 2에서 해결하지 않는다.

## 완료 조건

- [x] Phase 1의 `products/products-baseline` 원본 결과를 비교 기준으로 사용했다.
- [x] `loadtest` seed와 `pool10` Spring profile을 측정 조건으로 고정했다.
- [x] 측정 전후 `product` table의 row count, index 목록, 상태 분포를 evidence로 저장했다.
- [x] 상품 검색 대표 SQL의 인덱스 적용 전 `EXPLAIN`과 `EXPLAIN (ANALYZE, BUFFERS)` 결과를 기록했다.
- [x] `idx_product_category_status` 적용 후 실행계획과 실행시간 변화를 기록했다.
- [x] 같은 `loadtest`, `pool10`, `products baseline`, `strategy=baseline` 조건에서 k6를 실행했다.
- [x] k6 evidence run에 `measurement.json`, `k6-summary.json`, `k6-summary.txt`, `k6-exit-status.txt`, `run-window.json`이 포함됐다.
- [x] `pg_stat_statements.mean_exec_time`, k6 p95, p99, failed, dropped iterations를 Phase 1 product baseline과 비교했다.
- [x] Grafana screenshot은 Phase 2 실행 시간대의 보조 evidence로 저장했다.
- [x] 단일 인덱스 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스는 SQL-only 보조 실험으로 분리해 해석했다.
- [x] 남아 있는 병목이 쿼리 수 문제인지 확인하고 Phase 3으로 넘길 근거를 기록했다.
