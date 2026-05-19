# Phase 2 결과 보고서

> 목적: Phase 1 상품 검색 Baseline과 Phase 2 post-index 측정값을 비교하고, SQL-only 보조 실험에서 얻은 인덱스 설계 인사이트를 정리한다.

## 결론

Phase 2는 기존 `GET /api/products?categoryId=&status=` 상품 API 흐름을 유지한 채 PostgreSQL 인덱스만 변경했다. 메인 인덱스는 `idx_product_category_status ON product(category_id, status)`이며, pre-index 실행계획은 `Seq Scan on product`, post-index 실행계획은 `Bitmap Index Scan on idx_product_category_status` + `Bitmap Heap Scan on product`로 전환됐다. 아래 비교 표는 같은 `loadtest`, `pool10`, `products baseline` 조건에서 측정한 API와 SQL 값을 기록한다.

같은 `loadtest`, `pool10`, `products baseline` 조건에서 API p95는 17.24ms에서 12.14ms로 낮아졌고, `pg_stat_statements` 기준 SQL 평균 실행시간은 7.90ms에서 0.40ms로 낮아졌다. 이 결과는 Phase 1 상품 검색 병목이 애플리케이션 경로 변경 없이 product 필터 인덱스로 줄어드는 것을 보여준다.

SQL-only 보조 실험은 API latency와 직접 비교하지 않고, 인덱스 선택 원리를 설명하는 concept evidence로 분리한다.

## Phase 1 대비 Phase 2 비교

| Metric | Phase 1 Baseline | Phase 2 Post-index |
|---|---:|---:|
| API p95 | 17.24ms | 12.14ms |
| SQL mean time | 7.90ms | 0.40ms |
| SQL total time | 118,489.64ms | 5,986.88ms |
| requests | 15,001 | 15,001 |
| failed | 0.00% | 0.00% |
| dropped iterations | 0 | 0 |

Phase 2 post-index `pg_stat_statements`에서 product query는 `calls=15001`, `rows=2476309`로 기록됐다. k6 summary에는 `dropped_iterations` 항목이 별도 출력되지 않았고, 최종 상태가 `15001 complete and 0 interrupted iterations`였으므로 dropped iterations는 0으로 기록한다.

## Evidence

- [pre-index EXPLAIN](../../evidence/phase-02/products/pre-index/explain.txt)
- [post-index EXPLAIN](../../evidence/phase-02/products/pool10-post-index/explain.txt)
- [post-index k6 summary](../../evidence/phase-02/products/pool10-post-index/k6-summary.txt)
- [post-index pg_stat_statements](../../evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt)
- [Grafana screenshot](../../evidence/phase-02/grafana-screenshots/products-post-index.png)
- [SQL-only evidence index](../../evidence/phase-02/README.md)

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

## Phase 3 Handoff

Phase 2는 상품 검색 쿼리의 인덱스 설계와 실행계획 분석을 다뤘다. 주문 목록 조회의 반복적인 `order_item where order_id = ?` 호출은 Phase 1에서 이미 별도 병목으로 확인됐고, 인덱스 실험의 직접 해결 대상이 아니다.

따라서 다음 병목은 쿼리 수 문제로 분류하고 Phase 3 N+1 / 로딩 전략 최적화에서 다룬다.
