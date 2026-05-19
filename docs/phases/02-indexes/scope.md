# Phase 2 범위

## 목표

Phase 2의 목표는 Phase 1 상품 검색 Baseline과 비교 가능한 인덱스 전후 Evidence를 남기는 것이다.

- Phase 1 `products/pool10-baseline`과 비교 가능한 pre/post Evidence를 수집한다.
- 인덱스 적용 전 상품 검색 SQL의 `EXPLAIN (ANALYZE, BUFFERS)`를 기록한다.
- `idx_product_category_status ON product(category_id, status)`를 적용한다.
- 인덱스 적용 후 `EXPLAIN`, k6, `pg_stat_statements`, Grafana Evidence를 수집한다.
- 단일 컬럼 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스는 SQL-only 보조 실험으로 분리해 기록한다.

## 대상 API와 테이블

| 항목 | 값 |
|---|---|
| API | `GET /api/products?categoryId=&status=` |
| Repository | `ProductRepository.findByCategoryIdAndStatus` |
| Table | `product` |
| Main query shape | `product where category_id = ? and status = ?` |
| Main index | `idx_product_category_status ON product(category_id, status)` |

## Phase 1 비교 기준

| 항목 | 값 |
|---|---:|
| k6 scenario | `products` |
| preset | `baseline` |
| pool | `pool10` |
| request rate | 50 rps |
| API p95 | 17.24ms |
| SQL mean time | 7.90ms |
| SQL total time | 118,489.64ms |
| requests | 15,001 |
| failed | 0.00% |

## 제외 범위

- API/Controller/Service/Repository 성능 개선은 하지 않는다.
- DTO projection, QueryDSL, fetch join, batch loading은 적용하지 않는다.
- Flyway/Liquibase migration은 도입하지 않는다.
- SQL-only 보조 실험 결과를 API/k6 latency와 직접 비교하지 않는다.
- 주문 목록 N+1과 pagination 병목은 Phase 2에서 해결하지 않는다.

## 완료 조건

- [ ] Phase 1의 `products/pool10-baseline` 결과를 비교 기준으로 사용했다.
- [ ] 상품 검색 대표 SQL의 인덱스 적용 전 `EXPLAIN (ANALYZE, BUFFERS)` 결과를 기록했다.
- [ ] `idx_product_category_status` 적용 후 실행계획과 실행시간 변화를 기록했다.
- [ ] 같은 `loadtest`, `pool10`, `products baseline` 조건에서 k6를 다시 실행했다.
- [ ] `pg_stat_statements.mean_exec_time`, k6 p95, Grafana 지표를 Phase 1 결과와 비교했다.
- [ ] 단일 인덱스 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스는 SQL-only 보조 실험으로 분리해 해석했다.
- [ ] 남아 있는 병목이 쿼리 수 문제인지 확인하고 Phase 3으로 넘길 근거를 기록했다.
