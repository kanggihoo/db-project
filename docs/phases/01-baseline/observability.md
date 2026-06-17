# Phase 1 관측 전략

> 목적: k6 시나리오별로 k6 summary, PostgreSQL `pg_stat_statements`, SQL 실행계획에서 어떤 지표를 봐야 하는지 정리한다.

## 관측 원칙

Phase 1에서는 세 API를 섞지 않고 먼저 단일 시나리오로 측정한다.

| 시나리오 | 확인하려는 병목 |
|---|---|
| `orders` | N+1 쿼리 증가와 HikariCP 점유 |
| `products` | 인덱스 없는 필터 조회의 Seq Scan |
| `points` | no-index page0 운영 한계와 count query 비용 |

각 시나리오 직전에는 DB 통계를 초기화한다.

```sql
SELECT pg_stat_statements_reset();
VACUUM ANALYZE;
```

## 공통 지표

모든 시나리오는 k6 summary와 PostgreSQL snapshot을 기본 근거로 본다.

| 지표 | 출처 | 의미 |
|---|---|---|
| 요청 수, RPS | k6 summary | 실제로 주입된 부하 규모 |
| `http_req_failed` | k6 summary | timeout, 5xx, check 실패 비율 |
| p95, p99 | k6 summary | 사용자 관점 응답 지연 |
| dropped iterations | k6 summary | 목표 arrival rate를 맞추지 못한 반복 수 |
| calls, mean time, total time | `pg_stat_statements` | 반복 SQL의 비용과 누적 DB 시간 |
| actual time, rows, buffers | `EXPLAIN ANALYZE` | 실행계획과 실제 처리량 |

## 시나리오: orders

실행 예시:

```bash
./scripts/server.sh pool10
./k6/run.sh orders baseline
```

목표는 주문 목록 조회의 N+1이 요청당 SQL 수와 커넥션 점유 시간에 어떤 영향을 주는지 확인하는 것이다.

중점 지표:

| 지표 | 기대되는 관찰 |
|---|---|
| k6 p95/p99 | RPS 증가 시 계단식으로 튈 수 있음 |
| pg_stat_statements calls | `order_item where order_id = ?` 호출 수가 많아짐 |
| pg_stat_statements total_exec_time | 반복되는 단건 조회가 상위권에 올라옴 |

해석 기준:

| 관찰 | 해석 |
|---|---|
| `order_item` 조회 calls가 요청 수보다 훨씬 큼 | N+1 재현 성공 |
| `order_item` total_exec_time이 상위권 | 반복 단건 조회가 DB 시간을 지배 |
| `data_received=0`, 실패율 100% | HTTP latency 비교값이 아니라 앱 응답 실패 evidence |

증빙으로 남길 것:

- k6 summary
- `order_item` 조회가 포함된 `pg_stat_statements`
- 요청 1건당 추가 SQL 수를 보여주는 테스트 또는 로그

## 시나리오: products

실행 예시:

```bash
./scripts/server.sh pool10
STRATEGY=baseline ./k6/run.sh products baseline
STRATEGY=baseline ./k6/run.sh products stress-100
```

목표는 `category_id + status` 필터가 인덱스 없이 전체 테이블을 스캔하는지 확인하는 것이다.

중점 지표:

| 지표 | 기대되는 관찰 |
|---|---|
| pg_stat_statements mean_exec_time | 상품 조회 쿼리의 평균 실행시간 확인 |
| pg_stat_statements total_exec_time | 상품 조회가 총 DB 시간 상위권에 올라옴 |
| k6 p95/p99 | RPS 증가 시 테이블 스캔 비용과 같이 상승 가능 |
| EXPLAIN rows removed | 필터 조건 때문에 버린 row 수 |

해석 기준:

| 관찰 | 해석 |
|---|---|
| `Seq Scan`과 rows removed가 높음 | 인덱스 없는 필터 조회 병목 |
| mean_exec_time이 높고 calls가 많음 | 인덱스 없는 필터 조회 비용 누적 |
| Phase 2 인덱스 후 mean_exec_time 감소 | 인덱스 최적화 효과 증명 |
| Seq Scan에서 Index Scan으로 전환 | 실행 계획 전환 증명 |

증빙으로 남길 것:

- 상품 조회 쿼리의 `EXPLAIN ANALYZE`
- `pg_stat_statements` 상위 쿼리
- k6 p95/TPS/error rate

## 시나리오: points

실행 예시:

```bash
./scripts/server.sh pool10
./k6/run.sh points phase1-points-page0
```

현재 Phase 1 evidence의 목표는 no-index `point_history` 조회가 page0부터 운영 한계에 도달하는지 확인하는 것이다. page0과 page500의 정밀 비교는 응답 성공률이 회복된 뒤 Phase 7에서 다시 측정한다.

페이지별 preset은 다음 의미를 갖는다.

| preset | 의미 |
|---|---|
| `phase1-points-page0` | Phase 7 cleanup 후 clean DB 얕은 페이지 기준선 |
| `phase1-points-page500` | Phase 7 cleanup 후 clean DB 깊은 페이지 선택 재측정용 |
| `baseline` | page 가중 랜덤 분포 |

`points-page0` preset은 Phase 7 hot user `707000` 조건에 맞춰져 있으므로, Phase 1 clean rerun에는 사용하지 않는다.

중점 지표:

| 지표 | 기대되는 관찰 |
|---|---|
| k6 failure rate | page0에서도 timeout/check 실패가 발생하는지 확인 |
| k6 p95/p99 | timeout 상한에 도달하는지 확인 |
| dropped iterations | 목표 RPS를 따라가지 못하는지 확인 |
| pg_stat_statements mean_exec_time | page query와 count query의 평균 실행시간 |
| pg_stat_statements total_exec_time | page query와 count query의 누적 DB 시간 |

해석 기준:

| 관찰 | 해석 |
|---|---|
| page0부터 실패율이 높음 | no-index `Page<T>` 조회가 운영 한계에 도달 |
| page query와 count query가 모두 상위권 | cursor 전환뿐 아니라 count 제거/분리도 검토 필요 |
| `data_received=0`, 실패율 100% | 성능 비교값이 아니라 앱 응답 실패 evidence |
| Cursor/index/count 개선 후 page0 성공률 회복 | Phase 7 개선 효과의 1차 기준 |

증빙으로 남길 것:

- fixed page0 k6 summary
- page query와 count query가 포함된 `pg_stat_statements`
- `point_history` 인덱스 상태와 row count
- hot user의 point_history count

## Hikari Pool 비교 전략

Hikari pool은 서버 재시작 후 적용된다. pool 비교는 같은 데이터, 같은 k6 preset, 같은 DB 상태에서 반복한다.

권장 순서:

```text
pool10 baseline 측정
pool5 같은 시나리오 반복
pool20 같은 시나리오 반복
```

pool 비교를 수행할 때도 최종 판정은 k6 summary와 `pg_stat_statements`를 기준으로 한다.

## report.md에 기록할 값

각 시나리오마다 다음 값을 남긴다.

| 항목 | 출처 |
|---|---|
| scenario / preset / pool | 실행 명령 |
| 데이터 규모 | seed profile |
| k6 p50/p95/p99 | k6 summary |
| TPS | k6 `http_reqs` rate |
| error rate | k6 `http_req_failed` |
| top SQL mean/total time | pg_stat_statements |
| 원인 해석 | report.md |

## Phase 전환 기준

Phase 2로 넘어가기 전 최소 조건:

- `orders`, `products`, fixed `points page0` 결과가 각각 저장되어 있다.
- `orders`에서 N+1 호출 증가가 확인됐다.
- `products`에서 Seq Scan 또는 비효율 실행계획이 확인됐다.
- fixed `points page0`에서 no-index `Page<T>` 조회가 운영 한계에 도달함을 설명하는 근거가 있다.
- k6 결과와 SQL 분석 결과가 `report.md` 또는 동등한 문서에 기록됐다.
