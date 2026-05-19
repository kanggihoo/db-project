# Phase 2 관측 전략

> 목적: 인덱스 적용 전후 실행계획, SQL 실행시간, API latency, Grafana 지표를 같은 기준으로 해석한다.

## EXPLAIN 해석 기준

메인 쿼리와 SQL-only 보조 실험은 `EXPLAIN (ANALYZE, BUFFERS)`로 확인한다.

| Marker | 해석 |
|---|---|
| `Seq Scan` | 테이블을 순차 스캔한다. Phase 1 상품 검색 Baseline의 비교 기준이다. |
| `Index Scan` | 인덱스로 조건에 맞는 tuple을 찾은 뒤 테이블 row를 읽는다. |
| `Bitmap Index Scan` | 인덱스로 후보를 모은 뒤 bitmap 기반으로 heap을 읽는다. |
| `Index Only Scan` | 필요한 컬럼을 인덱스에서 충족할 수 있을 때 가능하다. visibility map 상태도 영향을 준다. |
| `Buffers` | shared hit/read 등 block 접근량을 보여준다. scan type 변화와 함께 읽는다. |
| `Execution Time` | PostgreSQL 내부에서 실제 실행에 걸린 시간이다. API latency와는 분리해 해석한다. |

## pg_stat_statements

post-index k6 실행 뒤 상품 검색 SQL row를 저장한다.

| Field | 확인 내용 |
|---|---|
| `calls` | 같은 SQL shape가 실행된 횟수 |
| `mean_exec_time` | SQL 1회 평균 실행시간 |
| `total_exec_time` | 전체 DB 시간에서 해당 SQL이 차지한 누적 시간 |
| `rows` | 반환 row 수와 요청 규모의 관계 |

Phase 1 상품 검색 Baseline row:

| 항목 | 값 |
|---|---:|
| calls | 15,001 |
| mean | 7.90ms |
| total | 118,489.64ms |
| rows | 2,508,501 |

## k6 지표

| Metric | 해석 |
|---|---|
| `http_req_duration p95` | API 요청 95%가 완료된 시간이다. Phase 1 p95 17.24ms와 비교한다. |
| `http_req_failed` | timeout, 5xx, expected response 실패율이다. Phase 1 상품 검색은 0.00%였다. |
| `dropped_iterations` | 목표 arrival rate를 맞추지 못해 실행하지 못한 iteration 수다. Phase 1 상품 검색은 0이었다. |

## Grafana 확인 행

공통 `DB Lab Overview` dashboard에서 다음 row를 확인한다.

| Row | 확인 내용 |
|---|---|
| Run Summary | phase/scenario/preset/pool 조건과 요청 처리 상태 |
| Table Access | `product`의 seq scan, index scan, tuple read 변화 |
| Hikari Pool | active connection, pending thread, acquire time |

## SQL-only 보조 실험 해석

SQL-only 보조 실험은 인덱스 개념 증거다. API performance evidence가 아니며, k6 p95나 Phase 1 API latency와 직접 비교하지 않는다.

| Topic | 관찰 기준 |
|---|---|
| 단일 컬럼 선택도 | `status = 'ON_SALE'`처럼 선택도가 낮은 조건에서 Seq Scan이 선택될 수 있는지 |
| 복합 인덱스 순서 | 왼쪽 컬럼, range 조건, `ORDER BY`가 인덱스 선택에 미치는 영향 |
| 커버링 인덱스 | 필요한 컬럼만 조회할 때 table access가 줄거나 `Index Only Scan`이 가능한지 |
| 부분 인덱스 | `is_deleted = false` 조건이 plan selection과 index size에 미치는 영향 |
