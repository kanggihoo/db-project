# Phase 2 결과 보고서

> 목적: Phase 1 상품 검색 Baseline과 Phase 2 post-index 측정값을 비교하고, SQL-only 보조 실험에서 얻은 인덱스 설계 인사이트를 정리한다.

## 결론

메인 상품 검색 쿼리는 `idx_product_category_status ON product(category_id, status)` 적용 후 실행계획이 `Seq Scan`에서 `Bitmap Index Scan` + `Bitmap Heap Scan`으로 전환됐다.

같은 `loadtest`, `pool10`, `products baseline` 조건에서 API p95는 17.24ms에서 12.14ms로 낮아졌고, `pg_stat_statements` 기준 SQL 평균 실행시간은 7.90ms에서 0.40ms로 낮아졌다. 이 결과는 Phase 1 상품 검색 병목이 애플리케이션 경로 변경 없이 product 필터 인덱스로 줄어드는 것을 보여준다.

SQL-only 보조 실험은 API latency와 직접 비교하지 않고, 인덱스 선택 원리를 설명하는 concept evidence로 분리한다. Grafana screenshot 이미지는 별도 캡처 대상으로 남긴다.

## Phase 1 기준값

| Metric | Value |
|---|---:|
| API p95 | 17.24ms |
| SQL mean | 7.90ms |
| SQL total | 118,489.64ms |
| requests | 15,001 |
| failed | 0.00% |
| dropped iterations | 0 |

## 메인 실험 결과

| Metric | Phase 1 baseline | Phase 2 post-index | 해석 |
|---|---:|---:|---|
| API p95 | 17.24ms | 12.14ms | 약 29.6% 감소했다. |
| SQL mean | 7.90ms | 0.40ms | 약 94.9% 감소했다. |
| SQL total | 118,489.64ms | 5,986.88ms | 같은 호출 수에서 누적 DB 시간이 크게 줄었다. |
| requests | 15,001 | 15,001 | 같은 요청 규모로 비교했다. |
| failed | 0.00% | 0.00% | 실패율은 계속 0%다. |
| dropped iterations | 0 | 0 | k6 summary에 `dropped_iterations`가 출력되지 않았고 목표 요청 수를 완료했다. |

Evidence:

- [pre-index EXPLAIN](../../evidence/phase-02/products/pre-index/explain.txt)
- [post-index EXPLAIN](../../evidence/phase-02/products/pool10-post-index/explain.txt)
- [post-index k6 summary](../../evidence/phase-02/products/pool10-post-index/k6-summary.txt)
- [post-index pg_stat_statements](../../evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt)

### 실행계획 비교

| Evidence | Scan type | Buffers | Execution Time | 해석 |
|---|---|---|---:|---|
| pre-index | `Seq Scan on product` | `shared hit=3109` | 13.869ms | 인덱스 없이 product 100,000건을 순차 스캔하고 99,606건을 필터링했다. |
| post-index | `Bitmap Index Scan` + `Bitmap Heap Scan` | `shared hit=366 read=2` | 1.584ms | `idx_product_category_status`로 후보 row를 먼저 찾고 필요한 heap block만 읽었다. |

post-index plan은 `Bitmap Index Scan on idx_product_category_status`를 사용했다. PostgreSQL이 단순 `Index Scan`이 아니라 bitmap 계열 plan을 선택했지만, Phase 2의 핵심인 `(category_id, status)` 조건 인덱스 사용과 buffer 접근 감소는 확인됐다.

## SQL-only 보조 실험 결과

| Topic | Evidence | 핵심 관찰 |
|---|---|---|
| Single-column index selectivity | [single-status-index.txt](../../evidence/phase-02/sql-only/single-status-index.txt) | `status = 'ON_SALE'` 조건은 약 79.8% row를 반환해 선택도가 낮다. `idx_product_status` 생성 후에도 planner는 `Seq Scan`을 유지했다. |
| Composite index order | [composite-order-index.txt](../../evidence/phase-02/sql-only/composite-order-index.txt) | `category_id` 단독 조건은 bitmap scan 후 sort가 필요했고, `category_id + status + ORDER BY created_at` 조건은 `idx_product_status_category_created`로 정렬을 포함한 index scan이 가능했다. `category_id + created_at range`는 `idx_product_category_status_created`의 prefix/range 조건을 사용했다. |
| Covering index | [covering-index.txt](../../evidence/phase-02/sql-only/covering-index.txt) | `idx_product_covering` 적용 후 `Index Only Scan`과 `Heap Fetches: 0`이 확인됐다. 필요한 column을 index에 포함하고 visibility map이 준비되면 heap 접근을 줄일 수 있다. |
| Partial index | [partial-index.txt](../../evidence/phase-02/sql-only/partial-index.txt) | `is_deleted = false` 조건을 포함한 partial index 적용 후 `Bitmap Index Scan on idx_product_active_category_status`로 전환됐다. 활성 product만 대상으로 하는 조건에서는 더 작은 인덱스가 후보 row 탐색을 줄인다. |

SQL-only 보조 실험은 API latency와 직접 비교하지 않는다. 각 실험은 인덱스 선택 원리를 설명하는 concept evidence로만 사용한다.

## Phase 3 handoff

Phase 2 완료 후에도 남는 반복 SQL, query count, 주문 목록 병목은 Phase 3의 N+1 최적화 대상으로 넘긴다. 특히 `orders`의 반복적인 `order_item` 단건 조회는 Phase 1에서 이미 별도 병목으로 분류되어 있으므로, 상품 검색 인덱스 결과와 섞어 판단하지 않는다.
