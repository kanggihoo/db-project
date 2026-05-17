# Phase 1 Observability Strategy

> 목적: k6 시나리오별로 Grafana와 PostgreSQL에서 어떤 지표를 봐야 하는지 정리한다.

## 관측 원칙

Phase 1에서는 세 API를 섞지 않고 먼저 단일 시나리오로 측정한다.

| 시나리오 | 확인하려는 병목 |
|---|---|
| `orders` | N+1 쿼리 증가와 HikariCP 점유 |
| `products` | 인덱스 없는 필터 조회의 Seq Scan |
| `points` | Offset deep page 지연 |

각 시나리오 직전에는 DB 통계를 초기화한다.

```sql
SELECT pg_stat_statements_reset();
VACUUM ANALYZE;
```

## 공통 지표

모든 시나리오에서 공통으로 보는 지표는 [Grafana Observability Guide](../../guides/grafana-observability.md)를 기준으로 한다.

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
| Hikari active connections | pool size 근처까지 상승할 수 있음 |
| Hikari pending threads | 커넥션을 기다리면 0보다 커짐 |
| Hikari acquire time | pending이 생기면 같이 증가 |
| k6 p95/p99 | RPS 증가 시 계단식으로 튈 수 있음 |
| pg_stat_statements calls | `order_item where order_id = ?` 호출 수가 많아짐 |
| pg_stat_statements total_exec_time | 반복되는 단건 조회가 상위권에 올라옴 |

해석 기준:

| 관찰 | 해석 |
|---|---|
| pending > 0, active가 pool 상한 근처 | 커넥션 풀 대기 병목 |
| pending = 0, p95 증가 | 풀보다 쿼리 형태, 반복 조회, 테이블 스캔, 데이터 규모가 병목 |
| `order_item` 조회 calls가 요청 수보다 훨씬 큼 | N+1 재현 성공 |
| pool20에서 p95가 줄어듦 | 풀 크기가 병목 완화에 일부 효과 |
| pool20에서도 p95가 그대로 높음 | N+1 쿼리 수 자체가 문제 |

증빙으로 남길 것:

- k6 summary
- Hikari active/pending 그래프
- `order_item` 조회가 포함된 `pg_stat_statements`
- 요청 1건당 추가 SQL 수를 보여주는 테스트 또는 로그

## 시나리오: products

실행 예시:

```bash
./scripts/server.sh pool10
./k6/run.sh products baseline
./k6/run.sh products stress-100
```

목표는 `category_id + status` 필터가 인덱스 없이 전체 테이블을 스캔하는지 확인하는 것이다.

중점 지표:

| 지표 | 기대되는 관찰 |
|---|---|
| pg_stat_statements mean_exec_time | 상품 조회 쿼리의 평균 실행시간 확인 |
| pg_stat_statements total_exec_time | 상품 조회가 총 DB 시간 상위권에 올라옴 |
| PostgreSQL seq scan count | product 테이블 스캔 증가 |
| PostgreSQL seq tuples read | product 테이블에서 읽은 tuple 증가 |
| PostgreSQL index scan count | Phase 2 인덱스 후 비교 기준 |
| Hikari pending threads | 없어도 응답시간은 느릴 수 있음 |
| k6 p95/p99 | RPS 증가 시 테이블 스캔 비용과 같이 상승 가능 |

해석 기준:

| 관찰 | 해석 |
|---|---|
| Hikari pending이 낮고 seq scan / seq tuples read가 높음 | 커넥션 풀이 아니라 풀스캔 병목 |
| mean_exec_time이 높고 calls가 많음 | 인덱스 없는 필터 조회 비용 누적 |
| Phase 2 인덱스 후 mean_exec_time 감소 | 인덱스 최적화 효과 증명 |
| seq scan count 감소, index scan 증가 | 실행 계획 전환 증명 |

증빙으로 남길 것:

- 상품 조회 쿼리의 `EXPLAIN ANALYZE`
- `pg_stat_statements` 상위 쿼리
- seq scan, index scan, seq tuples read 관련 Grafana 패널
- k6 p95/TPS/error rate

## 시나리오: points

실행 예시:

```bash
./scripts/server.sh pool10
./k6/run.sh points points-page0
./k6/run.sh points points-page500
```

목표는 같은 RPS에서 page가 깊어질수록 Offset 비용이 증가하는지 확인하는 것이다.

페이지별로 따로 측정한다.

| preset | 의미 |
|---|---|
| `points-page0` | 얕은 페이지 기준선 |
| `points-page500` | 깊은 페이지 병목 |
| `baseline` | page 가중 랜덤 분포 |

중점 지표:

| 지표 | 기대되는 관찰 |
|---|---|
| k6 p95/p99 | page500이 page0보다 높아야 함 |
| pg_stat_statements mean_exec_time | page500 쿼리 평균 실행시간 증가 |
| pg_stat_statements rows | 반환 row는 적어도 스캔 비용은 커짐 |
| PostgreSQL seq tuples read | deep page에서 읽는 tuple 증가 가능 |
| Hikari active connections | 쿼리가 느릴수록 커넥션 점유 시간이 길어짐 |

해석 기준:

| 관찰 | 해석 |
|---|---|
| page0은 빠르고 page500만 느림 | Offset deep page 병목 재현 |
| page500에서도 빠름 | hot user당 point_history 수가 부족하거나 데이터 규모 부족 |
| Hikari pending까지 증가 | deep page 쿼리가 커넥션을 오래 점유 |
| Cursor 전환 후 page500 p95 감소 | Phase 7 개선 효과 증명 |

증빙으로 남길 것:

- `points-page0` k6 summary
- `points-page500` k6 summary
- 두 테스트의 `pg_stat_statements` 비교
- hot user의 point_history count

## Hikari Pool 비교 전략

Hikari pool은 서버 재시작 후 적용된다. pool 비교는 같은 데이터, 같은 k6 preset, 같은 DB 상태에서 반복한다.

권장 순서:

```text
pool10 baseline 측정
pool5 같은 시나리오 반복
pool20 같은 시나리오 반복
```

상세 해석 기준은 [Grafana Observability Guide](../../guides/grafana-observability.md)의 Hikari pool 해석을 따른다.

## Grafana 패널 구성 권장

Phase 1 대시보드는 [Grafana Observability Guide](../../guides/grafana-observability.md)의 공통 패널을 먼저 구성하고, 시나리오별 패널은 이 문서의 각 섹션을 따른다.

## BASELINE.md에 기록할 값

각 시나리오마다 다음 값을 남긴다.

| 항목 | 출처 |
|---|---|
| scenario / preset / pool | 실행 명령 |
| 데이터 규모 | seed profile |
| k6 p50/p95/p99 | k6 summary |
| TPS | k6 `http_reqs` rate |
| error rate | k6 `http_req_failed` |
| Hikari max active | Grafana |
| Hikari pending max | Grafana |
| table seq/index scan delta | Grafana |
| seq tuples read delta | Grafana |
| top SQL mean/total time | pg_stat_statements |
| 원인 해석 | notes.md 또는 BASELINE.md |

## Phase 전환 기준

Phase 2로 넘어가기 전 최소 조건:

- `orders`, `products`, `points-page0`, `points-page500` 결과가 각각 저장되어 있다.
- `orders`에서 N+1 호출 증가가 확인됐다.
- `products`에서 Seq Scan 또는 비효율 실행계획이 확인됐다.
- `points-page500`이 `points-page0`보다 느린 근거가 있다.
- k6 결과와 SQL 분석 결과가 `BASELINE.md` 또는 동등한 문서에 기록됐다.
