# Phase 2 결과 보고서

> 목적: Phase 1 상품 검색 Baseline과 Phase 2 post-index 측정값을 비교하고, SQL-only 보조 실험에서 얻은 인덱스 설계 인사이트를 정리한다.

## 결론

측정 전이다. `idx_product_category_status ON product(category_id, status)` 적용 전후 Evidence를 수집한 뒤 이 결론을 갱신한다.

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
| API p95 | 17.24ms | TBD | TBD |
| SQL mean | 7.90ms | TBD | TBD |
| SQL total | 118,489.64ms | TBD | TBD |
| requests | 15,001 | TBD | TBD |
| failed | 0.00% | TBD | TBD |
| dropped iterations | 0 | TBD | TBD |

### 실행계획 비교

| Evidence | Scan type | Buffers | Execution Time | 해석 |
|---|---|---|---:|---|
| pre-index | TBD | TBD | TBD | TBD |
| post-index | TBD | TBD | TBD | TBD |

## SQL-only 보조 실험 결과

| Topic | Evidence | 핵심 관찰 |
|---|---|---|
| Single-column index selectivity | `sql-only/single-status-index.txt` | TBD |
| Composite index order | `sql-only/composite-order-index.txt` | TBD |
| Covering index | `sql-only/covering-index.txt` | TBD |
| Partial index | `sql-only/partial-index.txt` | TBD |

SQL-only 보조 실험은 API latency와 직접 비교하지 않는다. 각 실험은 인덱스 선택 원리를 설명하는 concept evidence로만 사용한다.

## Phase 3 handoff

Phase 2 완료 후에도 남는 반복 SQL, query count, 주문 목록 병목은 Phase 3의 N+1 최적화 대상으로 넘긴다. 특히 `orders`의 반복적인 `order_item` 단건 조회는 Phase 1에서 이미 별도 병목으로 분류되어 있으므로, 상품 검색 인덱스 결과와 섞어 판단하지 않는다.
