# Phase 2 관측 전략

> 목적: 인덱스 적용 전후 실행계획, SQL 실행시간, API latency, Grafana 지표를 같은 기준으로 해석한다.

## 원본 우선순위

최종 report의 정량 수치는 아래 순서로 사용한다.

1. k6 run-level 수치: `k6-summary.json`
2. 사람이 검토하는 k6 종료 summary: 같은 wrapper 실행에서 생성된 `k6-summary.txt`
3. DB 누적 SQL 시간: k6 직후 저장한 `pg_stat_statements`
4. 단건 실행계획: `EXPLAIN (ANALYZE, BUFFERS)`
5. Grafana screenshot: 같은 `run-window.json` 시간대의 보조 관측

Phase 1 Grafana screenshot은 별도 evidence로 남아 있지 않다. 따라서 Phase 1 대비 Phase 2의 정량 비교는 k6 summary와 `pg_stat_statements`로 수행하고, Grafana는 Phase 2 실행이 같은 조건으로 진행됐는지 확인하는 보조 자료로만 사용한다.

## EXPLAIN 해석 기준

메인 쿼리와 SQL-only 보조 실험은 `EXPLAIN (ANALYZE, BUFFERS)`로 확인한다.

| Marker | 해석 |
|---|---|
| `Seq Scan` | 테이블을 순차 스캔한다. 보조 인덱스가 없거나 planner가 인덱스 사용보다 순차 스캔을 더 싸게 판단한 경우다. |
| `Index Scan` | 인덱스로 조건에 맞는 tuple을 찾은 뒤 테이블 row를 읽는다. |
| `Bitmap Index Scan` | 인덱스로 후보 tuple 위치를 모은다. |
| `Bitmap Heap Scan` | bitmap 결과를 바탕으로 heap block을 읽는다. |
| `Index Only Scan` | 필요한 컬럼을 인덱스에서 충족할 수 있을 때 가능하다. visibility map 상태도 영향을 준다. |
| `Rows Removed by Filter` | 순차 스캔이나 heap scan에서 조건으로 버린 row 수다. |
| `Buffers` | shared hit/read 등 block 접근량을 보여준다. scan type 변화와 함께 읽는다. |
| `Execution Time` | PostgreSQL 내부에서 실제 실행에 걸린 시간이다. API latency와는 분리해 해석한다. |

메인 Phase 2 pre/post EXPLAIN은 같은 `category_id=10 AND status='ON_SALE'` 조건을 사용한다. Phase 1 EXPLAIN은 no-index baseline 설명에는 쓸 수 있지만, `category_id=1`과 cold read 성격이 섞여 있으므로 Phase 2 pre/post 단건 실행시간 비교와 직접 섞지 않는다.

## Phase 1 Product Baseline

Phase 1 전체 보고서의 핵심 결론은 points 조회 운영 한계다. Phase 2에서는 그중 product API 원본 evidence만 인덱스 비교 기준으로 사용한다.

| 항목 | 값 | 원본 |
|---|---:|---|
| requests | 14,935 | `phase-01/products/products-baseline/k6-summary.json` |
| request rate | 49.78/s | `phase-01/products/products-baseline/k6-summary.json` |
| k6 p95 | 621.91ms | `phase-01/products/products-baseline/k6-summary.json` |
| k6 p99 | 2065.98ms | `phase-01/products/products-baseline/k6-summary.json` |
| failed rate | 0.0134% | `phase-01/products/products-baseline/k6-summary.json` |
| dropped iterations | 66 | `phase-01/products/products-baseline/k6-summary.json` |
| SQL calls | 14,935 | `phase-01/products/products-baseline/pg-stat-statements.txt` |
| SQL mean | 21.45ms | `phase-01/products/products-baseline/pg-stat-statements.txt` |
| SQL total | 320,401.33ms | `phase-01/products/products-baseline/pg-stat-statements.txt` |

## Phase 2 Post-index 관측값

| 항목 | 값 | 원본 |
|---|---:|---|
| requests | 15,001 | `phase-02/products/pool10-post-index/k6-summary.json` |
| request rate | 50.00/s | `phase-02/products/pool10-post-index/k6-summary.json` |
| k6 avg | 17.29ms | `phase-02/products/pool10-post-index/k6-summary.json` |
| k6 p95 | 18.49ms | `phase-02/products/pool10-post-index/k6-summary.json` |
| k6 p99 | 81.38ms | `phase-02/products/pool10-post-index/k6-summary.json` |
| failed rate | 0.00% | `phase-02/products/pool10-post-index/k6-summary.json` |
| dropped iterations | 0 | `k6-summary.txt`의 complete/interrupted 상태 |
| SQL calls | 15,001 | `phase-02/products/pool10-post-index/pg-stat-statements.txt` |
| SQL mean | 9.13ms | `phase-02/products/pool10-post-index/pg-stat-statements.txt` |
| SQL total | 136,902.02ms | `phase-02/products/pool10-post-index/pg-stat-statements.txt` |

## pg_stat_statements

post-index k6 실행 뒤 상품 검색 SQL row를 저장한다.

| Field | 확인 내용 |
|---|---|
| `calls` | 같은 SQL shape가 실행된 횟수 |
| `mean_exec_time` | SQL 1회 평균 실행시간 |
| `total_exec_time` | 전체 DB 시간에서 해당 SQL이 차지한 누적 시간 |
| `rows` | 반환 row 수와 요청 규모의 관계 |

Phase 1과 Phase 2의 requests와 SQL calls가 거의 같기 때문에 product query의 mean/total time 변화는 비교 가능하다.

## k6 지표

| Metric | 원본 | 해석 |
|---|---|---|
| `http_reqs.count` | `k6-summary.json` | 완료된 HTTP 요청 수다. Phase 1 요청 수와 비교한다. |
| `http_req_duration.p(95)` | `k6-summary.json` | API 요청 95%가 완료된 시간이다. |
| `http_req_duration.p(99)` | `k6-summary.json` | tail latency 확인용 지표다. |
| `http_req_failed.value` | `k6-summary.json` | timeout, 5xx, expected response 실패율이다. |
| `dropped_iterations.count` | `k6-summary.json` 또는 `k6-summary.txt` | 목표 arrival rate를 맞추지 못해 시작하지 못한 iteration 수다. JSON에 값이 없으면 text summary의 complete/interrupted 상태를 함께 확인한다. |
| exit code | `k6-exit-status.txt` | `0`은 passed, `99`는 threshold_failed, 그 외는 execution_failed로 본다. |

## Grafana 확인 행

Grafana screenshot은 `docs/evidence/phase-02/products/pool10-post-index/run-window.json`의 `grafanaFrom`, `grafanaTo` 범위와 같은 시간대를 사용한다.

공통 `DB Lab Overview` dashboard에서 다음 row를 확인한다.

| Row | 확인 내용 |
|---|---|
| Run Summary | phase/scenario/preset/pool 조건과 요청 처리 상태 |
| k6 Load | RPS, p95 latency, checks, dropped iterations 흐름 |
| Spring Runtime | CPU, heap, GC pause time |
| Hikari Pool | active connection, pending thread, acquire time |
| PostgreSQL Activity | active sessions, lock, transaction 상태 |
| Table Access | `product`의 seq scan, index scan, tuple read 변화 |
| Phase 2 Index Focus | product scan과 latency 흐름 |

## SQL-only 보조 실험 해석

SQL-only 보조 실험은 인덱스 개념 증거다. API performance evidence가 아니며, k6 p95나 Phase 1 API latency와 직접 비교하지 않는다.

| Topic | 관찰 기준 |
|---|---|
| 단일 컬럼 선택도 | `status = 'ON_SALE'`처럼 반환 비율이 높은 조건에서 Seq Scan이 유지되는지 |
| 복합 인덱스 순서 | 왼쪽 컬럼, range 조건, `ORDER BY`가 인덱스 선택에 미치는 영향 |
| 커버링 인덱스 | 필요한 컬럼만 조회할 때 table access가 줄거나 `Index Only Scan`이 가능한지 |
| 부분 인덱스 | `is_deleted = false` 조건이 plan selection과 index size에 미치는 영향 |
