# Phase 2 결과 보고서

## 결론

Phase 2는 `GET /api/products?categoryId=&status=&strategy=baseline` 경로를 바꾸지 않고, `product(category_id, status)` 조건에 맞춘 PostgreSQL 복합 인덱스가 실행계획과 부하 지표를 어떻게 바꾸는지 확인했다.

메인 결과는 명확하다. `idx_product_category_status ON product(category_id, status)` 적용 후 대표 SQL은 `Seq Scan`에서 `Bitmap Index Scan on idx_product_category_status`와 `Bitmap Heap Scan`으로 바뀌었다. 같은 `loadtest`, `pool10`, `products baseline`, 50 rps, 5분 조건에서 k6 p95는 Phase 1 원본 기준 `621.91ms`에서 Phase 2 `18.49ms`로 낮아졌고, p99는 `2065.98ms`에서 `81.38ms`로 낮아졌다. `pg_stat_statements` 기준 상품 검색 SQL 평균 실행시간도 `21.45ms`에서 `9.13ms`로 낮아졌다.

다만 Phase 1의 전체 결론은 point 조회가 page0부터 운영 한계에 도달했다는 것이었다. Phase 2 report에서는 Phase 1 전체 결론을 product API로 과장하지 않고, Phase 1의 `products/products-baseline` 원본 evidence만 상품 검색 인덱스 비교 기준으로 사용한다. Phase 1 Grafana screenshot은 별도 evidence로 남아 있지 않으므로, Grafana는 Phase 2 실행 시간대의 보조 관측으로만 사용한다.

## 실험 조건

| 항목 | 값 |
|---|---|
| 비교 기준 | Phase 1 `products/products-baseline` 원본 evidence |
| Phase 2 condition | `pool10-post-index` |
| API | `GET /api/products?categoryId=&status=&strategy=baseline` |
| SQL shape | `product where category_id = ? and status = ?` |
| seed | `loadtest` |
| Spring profile / pool | `pool10` |
| k6 scenario / preset | `products` / `baseline` |
| workload | 50 rps, 5분, timeout 5초, preAllocatedVUs 100, maxVUs 300 |
| Phase 2 main index | `idx_product_category_status ON product(category_id, status)` |
| API 코드 변경 | 없음 |

Phase 2 measurement manifest는 `phase=phase-02`, `scenario=products`, `condition=pool10-post-index`, `preset=baseline`, `pool=pool10`, `mode=prometheus`를 기록한다. 대상 query parameter는 `categoryId=1..200`, `status=ON_SALE/SOLD_OUT/DISCONTINUED`, `strategy=baseline`이다.

## 데이터와 테이블 상태

`loadtest` 데이터에서 `product`는 100,000건이다. 인덱스 적용 전에는 `product_pkey`만 있었고, 인덱스 적용 후에는 `idx_product_category_status`가 추가됐다. 대표 EXPLAIN 쿼리의 `category_id=10 AND status='ON_SALE'` 조건은 394건을 반환한다.

`product.status`는 `ON_SALE`, `SOLD_OUT`, `DISCONTINUED` 값만 허용하는 check constraint를 갖고, `product.category_id`는 `category(id)`를 참조한다. 따라서 이번 Phase의 핵심 조건인 `category_id`, `status`는 실제 제약조건과 데이터 분포가 확인된 컬럼이다.

## Phase 1 대비 Phase 2 결과

| 지표 | Phase 1 product baseline | Phase 2 post-index | 변화 |
|---|---:|---:|---:|
| requests | 14,935 | 15,001 | +0.4% |
| request rate | 49.78/s | 50.00/s | +0.4% |
| k6 p95 | 621.91ms | 18.49ms | 97.0% 감소 |
| k6 p99 | 2065.98ms | 81.38ms | 96.1% 감소 |
| failed rate | 0.0134% | 0.00% | 감소 |
| dropped iterations | 66 | 0 | 감소 |
| SQL calls | 14,935 | 15,001 | +0.4% |
| SQL mean time | 21.45ms | 9.13ms | 57.4% 감소 |
| SQL total time | 320,401.33ms | 136,902.02ms | 57.3% 감소 |

Phase 2의 k6 종료 summary는 `http_req_duration p(95)=18.49ms`, `p(99)=81.38ms`, `http_req_failed=0.00%`, `http_reqs=15001`을 기록했다. `dropped_iterations` metric은 JSON에 값이 없고, text summary 마지막 상태가 `15001 complete and 0 interrupted iterations`이므로 dropped iterations는 0으로 해석한다. k6 exit status는 0이다.

`pg_stat_statements`에서는 상품 검색 SQL이 `15001`회 호출됐고, 평균 `9.13ms`, 누적 `136,902.02ms`, 반환 row `2,526,504`로 기록됐다. Phase 1의 같은 SQL shape는 `14935`회 호출, 평균 `21.45ms`, 누적 `320,401.33ms`였다. 요청 수가 거의 같기 때문에 SQL 평균과 누적 시간 비교는 유효하다.

SQL 평균 실행시간 감소폭과 API p95 감소폭은 1:1로 해석하지 않는다. SQL mean은 상품 검색 SQL 1회의 DB 내부 실행시간이고, k6 p95는 애플리케이션 처리, 커넥션 풀 대기, 네트워크, 런타임 상태, tail latency가 모두 포함된 HTTP 요청 분포다. 따라서 Phase 2의 강한 주장은 "SQL mean이 57.4% 줄어서 API p95가 정확히 97.0% 줄었다"가 아니라, 같은 요청 규모에서 실행계획이 인덱스 기반으로 바뀌었고 SQL 평균/누적 시간과 API p95/p99가 같은 방향으로 개선됐다는 점이다.

## 실행계획 비교

| Evidence | Plan | Rows | 주요 buffer | Execution Time | 해석 |
|---|---|---:|---|---:|---|
| Phase 2 pre-index | `Seq Scan on product` | 394 | `shared hit=3110` | 8.525ms | 100,000건 중 99,606건을 필터로 제거했다. |
| Phase 2 post-index | `Bitmap Index Scan` + `Bitmap Heap Scan` | 394 | heap `shared hit=367`, index `shared hit=5` | 0.833ms | `(category_id, status)` 조건으로 후보 row를 먼저 찾은 뒤 필요한 heap block만 읽었다. |

Phase 1 product EXPLAIN도 `Seq Scan`이었다. 다만 Phase 1 EXPLAIN은 `category_id=1` 조건이고 `Buffers: shared hit=4 read=3106`으로 cold read 성격이 섞여 있다. Phase 2 pre/post EXPLAIN은 `category_id=10` 조건으로 같은 Phase 2 수집 흐름에서 찍은 전후 비교다. 따라서 단건 EXPLAIN의 실행시간 전후 비교는 Phase 2 pre/post 파일을 기준으로 보고, Phase 1은 부하 기준선과 no-index baseline을 설명하는 근거로 사용한다.

실행계획 관점에서 이번 인덱스는 이론상 타당하게 작동했다. 단순 `Index Scan`이 아니라 bitmap 계열 plan을 선택했지만, 목표는 특정 plan 이름을 강제하는 것이 아니라 `category_id`와 `status` 조건을 보조 인덱스로 처리해 full table scan을 피하는 것이다. post-index plan은 `idx_product_category_status`의 `Index Cond`를 사용하므로 목표에 부합한다.

## SQL-only 보조 실험

SQL-only 결과는 API latency와 직접 비교하지 않는다. 이 실험들은 PostgreSQL planner가 인덱스를 선택하는 조건을 설명하는 보조 evidence다.

| Topic | 핵심 관찰 | 해석 |
|---|---|---|
| 단일 컬럼 선택도 | `status='ON_SALE'`는 79,773건을 반환했고, `idx_product_status` 생성 후에도 `Seq Scan`을 유지했다. | 선택도가 낮은 단일 컬럼 인덱스는 있어도 planner가 사용하지 않을 수 있다. |
| 복합 인덱스 순서 | `category_id` 단독 조건은 bitmap scan 후 sort가 필요했고, `category_id + status + ORDER BY created_at` 조건은 복합 인덱스의 순서 차이를 드러냈다. | 동등 조건, range 조건, 정렬 요구에 따라 적합한 복합 인덱스 순서가 달라진다. |
| 커버링 인덱스 | `idx_product_covering` 적용 후 `Index Only Scan`, `Heap Fetches: 0`, `Execution Time=0.067ms`가 확인됐다. | API가 전체 Entity를 조회하는 현재 경로에는 바로 적용하지 않았지만, 필요한 컬럼만 읽는 쿼리에서는 heap 접근을 제거할 수 있다. |
| 부분 인덱스 | `is_deleted=false` 조건이 포함된 쿼리는 partial index 적용 후 `Bitmap Index Scan on idx_product_active_category_status`로 바뀌었다. | 자주 쓰는 고정 조건이 쿼리에 포함된다면 partial index로 인덱스 범위를 줄일 수 있다. |

## Grafana 관측

Phase 2 Grafana screenshot은 `run-window.json`의 `grafanaFrom=1781680262548`, `grafanaTo=1781680594367` 범위로 캡처됐다. capture metadata의 dashboard variable도 `phase=phase-02`, `scenario=products`, `preset=baseline`, `pool=pool10`, `table=product`를 가리킨다.

Grafana에서는 실행 중 p95, error rate, dropped iterations, Hikari pending, table access row를 확인할 수 있다. 다만 Phase 1 Grafana screenshot이 없으므로 전후 정량 비교 표에는 Grafana 값을 사용하지 않는다. 최종 수치 비교는 k6 summary와 `pg_stat_statements`를 기준으로 한다.

## 결론과 다음 Phase

Phase 2의 결론은 product 필터 조회에 `(category_id, status)` 복합 인덱스가 필요하고, 실제 API 부하에서도 p95, p99, SQL 평균 실행시간을 낮춘다는 것이다. 이 결과는 API 구현 변경 없이 DB 인덱스만으로 얻은 변화다.

남은 병목은 product 필터 단일 쿼리보다 다른 조회 경로의 쿼리 수 문제다. Phase 1에서 주문 목록은 반복 SQL 호출 문제가 확인됐고, Phase 2의 product 인덱스 실험은 그 문제를 해결하는 범위가 아니다. 따라서 Phase 3은 N+1과 로딩 전략 최적화로 넘어가는 것이 타당하다.

## 한계와 보완사항

- Phase 1 전체 결론은 points 조회 운영 한계였고, product 결과는 Phase 2 비교를 위한 subset baseline이다.
- Phase 1 Grafana screenshot은 현재 evidence에 없으므로 Grafana 전후 정량 비교는 하지 않는다.
- Phase 2 measurement의 `git.dirty=true`는 남겨 둔다. 현재 evidence는 실행 조건, k6 summary, `pg_stat_statements`, EXPLAIN, run-window가 서로 맞기 때문에 report 작성에는 충분하다. 다만 외부 공유나 장기 보존용 release artifact가 필요하면 clean commit 기준으로 같은 명령을 재실행하는 편이 낫다.
- k6 부하 실행은 단일 대표 실행이다. 더 엄격한 통계가 필요하면 같은 조건을 3회 이상 반복하고 중앙값 또는 분산을 함께 기록한다.

## 근거 링크

- Phase 1 product k6: [k6-summary.json](../../evidence/phase-01/products/products-baseline/k6-summary.json)
- Phase 1 product SQL snapshot: [pg-stat-statements.txt](../../evidence/phase-01/products/products-baseline/pg-stat-statements.txt)
- Phase 1 product EXPLAIN: [products-baseline-explain.txt](../../evidence/phase-01/explain/products-baseline-explain.txt)
- Phase 2 DB state: [pre-index product state](../../evidence/phase-02/db-state/10-pre-index-product-state.txt), [post-index product state](../../evidence/phase-02/db-state/20-post-index-product-state.txt)
- Phase 2 EXPLAIN: [pre-index](../../evidence/phase-02/products/pre-index/explain.txt), [post-index](../../evidence/phase-02/products/pool10-post-index/explain.txt)
- Phase 2 k6: [k6-summary.json](../../evidence/phase-02/products/pool10-post-index/k6-summary.json), [k6-summary.txt](../../evidence/phase-02/products/pool10-post-index/k6-summary.txt), [run-window.json](../../evidence/phase-02/products/pool10-post-index/run-window.json)
- Phase 2 SQL snapshot: [pg-stat-statements.txt](../../evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt)
- Phase 2 Grafana screenshot: [products-post-index.png](../../evidence/phase-02/grafana-screenshots/products-post-index.png)
- SQL-only evidence: [single status](../../evidence/phase-02/sql-only/single-status-index.txt), [composite order](../../evidence/phase-02/sql-only/composite-order-index.txt), [covering](../../evidence/phase-02/sql-only/covering-index.txt), [partial](../../evidence/phase-02/sql-only/partial-index.txt)
