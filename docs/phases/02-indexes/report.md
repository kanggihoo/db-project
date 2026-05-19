# Phase 2 결과 보고서

> 목적: Phase 1 상품 검색 Baseline과 Phase 2 post-index 측정값을 비교하고, SQL-only 보조 실험으로 인덱스 설계 원리를 정리한다.

## 결론

Phase 2는 기존 `GET /api/products?categoryId=&status=` API 흐름을 바꾸지 않고 PostgreSQL 인덱스만 적용했다. 메인 인덱스는 `idx_product_category_status ON product(category_id, status)`이며, 대표 product 필터 쿼리의 실행계획은 `Seq Scan on product`에서 `Bitmap Index Scan on idx_product_category_status` + `Bitmap Heap Scan on product`로 전환됐다.

같은 `loadtest`, `pool10`, `products baseline` 조건에서 API p95는 17.24ms에서 12.14ms로 낮아졌고, `pg_stat_statements` 기준 SQL 평균 실행시간은 7.90ms에서 0.40ms로 낮아졌다. Phase 2의 핵심 결과는 상품 검색 병목이 애플리케이션 경로 변경 없이 product 필터 인덱스로 줄어든다는 점이다.

SQL-only 보조 실험은 API latency와 직접 비교하지 않는다. 해당 결과는 단일 컬럼, 복합 순서, covering index, partial index가 planner 선택에 어떤 차이를 만드는지 설명하는 concept evidence로 사용한다.

## 측정 조건

| 항목 | 값 |
|---|---|
| 기준 Baseline | Phase 1 `products/pool10-baseline` |
| Phase 2 workload | `products baseline` |
| k6 rate | 50 rps |
| k6 duration | 5분 |
| connection pool | `pool10` |
| 대상 API | `GET /api/products?categoryId=&status=` |
| 대상 table | `product` |
| 메인 인덱스 | `idx_product_category_status ON product(category_id, status)` |
| API 변경 여부 | 없음 |

## Phase 1 대비 Phase 2 비교

| Metric | Phase 1 Baseline | Phase 2 Post-index | 변화 |
|---|---:|---:|---:|
| API p95 | 17.24ms | 12.14ms | 29.6% 감소 |
| SQL mean time | 7.90ms | 0.40ms | 94.9% 감소 |
| SQL total time | 118,489.64ms | 5,986.88ms | 94.9% 감소 |
| requests | 15,001 | 15,001 | 동일 |
| failed | 0.00% | 0.00% | 동일 |
| dropped iterations | 0 | 0 | 동일 |

Phase 2 post-index k6 summary는 `http_req_duration p(95)=12.14ms`, `http_req_failed=0.00%`, `http_reqs=15001`을 기록했다. `dropped_iterations` 항목은 별도 출력되지 않았고, 최종 상태가 `15001 complete and 0 interrupted iterations`였으므로 dropped iterations는 0으로 기록한다.

Phase 2 post-index `pg_stat_statements`에서 product query는 `calls=15001`, `mean_ms=0.40`, `total_ms=5986.88`, `rows=2476309`로 기록됐다.

## 실행계획 비교

| Evidence | Plan | Rows | Buffers | Execution Time | 해석 |
|---|---|---:|---|---:|---|
| pre-index | `Seq Scan on product` | 394 | `shared hit=3109` | 13.869ms | product 100,000건을 순차 스캔하고 99,606건을 필터링했다. |
| post-index | `Bitmap Index Scan` + `Bitmap Heap Scan` | 394 | `shared hit=366 read=2` | 1.584ms | `idx_product_category_status`로 후보 row를 먼저 찾고 필요한 heap block만 읽었다. |

post-index plan은 단순 `Index Scan`이 아니라 bitmap 계열 plan을 선택했다. 이 planner 선택은 Phase 2의 목표와 충돌하지 않는다. 핵심은 `(category_id, status)` 조건을 인덱스로 처리하면서 heap 접근 범위와 실행시간이 크게 줄었다는 점이다.

## SQL-only 보조 실험 결과

| Topic | Evidence | 핵심 관찰 | 해석 |
|---|---|---|---|
| Single-column status index | [single-status-index.txt](../../evidence/phase-02/sql-only/single-status-index.txt) | `status = 'ON_SALE'`는 79,773건을 반환했고, `idx_product_status` 생성 후에도 `Seq Scan`을 유지했다. | 반환 비율이 높아 선택도가 낮은 단일 컬럼 인덱스는 planner가 사용하지 않을 수 있다. |
| Composite index order | [composite-order-index.txt](../../evidence/phase-02/sql-only/composite-order-index.txt) | `category_id` 단독 조건은 bitmap scan 후 sort가 필요했고, `category_id + status + ORDER BY created_at` 조건은 `idx_product_status_category_created`를 사용했다. | 복합 인덱스 순서는 동등 조건, range 조건, 정렬 요구가 함께 있을 때 차이를 만든다. |
| Covering index | [covering-index.txt](../../evidence/phase-02/sql-only/covering-index.txt) | `idx_product_covering` 적용 후 `Index Only Scan`과 `Heap Fetches: 0`이 확인됐다. | 조회 컬럼을 index에 포함하고 visibility map이 준비되면 heap 접근을 제거할 수 있다. |
| Partial index | [partial-index.txt](../../evidence/phase-02/sql-only/partial-index.txt) | `is_deleted = false` 조건이 있는 쿼리는 `idx_product_active_category_status`를 사용해 `Bitmap Index Scan`으로 전환됐다. | 자주 쓰는 고정 조건을 partial index predicate로 제한하면 더 작은 인덱스로 후보 row를 찾을 수 있다. |

SQL-only 결과는 메인 API 실험의 latency 개선 수치와 직접 합산하거나 비교하지 않는다. 메인 실험은 실제 API 경로의 before/after evidence이고, SQL-only 실험은 planner 선택 원리를 설명하는 보조 evidence다.

## Evidence

- [pre-index EXPLAIN](../../evidence/phase-02/products/pre-index/explain.txt)
- [post-index EXPLAIN](../../evidence/phase-02/products/pool10-post-index/explain.txt)
- [post-index k6 summary](../../evidence/phase-02/products/pool10-post-index/k6-summary.txt)
- [post-index pg_stat_statements](../../evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt)
- [Grafana screenshot](../../evidence/phase-02/grafana-screenshots/products-post-index.png)
- [SQL-only evidence index](../../evidence/phase-02/README.md)

## Phase 3 Handoff

Phase 2는 상품 검색 쿼리의 인덱스 설계와 실행계획 분석을 다뤘다. 주문 목록 조회에서 반복되는 `order_item where order_id = ?` 호출은 Phase 1에서 이미 별도 병목으로 확인됐고, product 인덱스 실험의 직접 해결 대상이 아니다.

따라서 다음 병목은 단일 쿼리 실행시간 문제가 아니라 쿼리 수 문제로 분류한다. Phase 3에서는 N+1 / 로딩 전략 최적화로 주문 목록 조회의 반복 SQL을 줄이는 방향을 다룬다.
