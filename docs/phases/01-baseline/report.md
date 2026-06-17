# Phase 1 베이스라인 보고서

Phase 1은 최적화하지 않은 구현을 실제 부하에서 측정해 이후 Phase의 비교 기준을 만드는 단계다. 이번 보고서의 핵심 결론은 `points`에서 정밀한 page0 vs page500 차이를 주장하는 것이 아니다. 현재 증거가 말하는 결론은 더 단순하다. 보조 인덱스가 없는 `point_history` 조회는 page0부터 운영 한계에 도달했다.

## 결론

`point_history`는 2,000,000건이고 인덱스는 `point_history_pkey` 1개뿐이다. 이 상태에서 `GET /api/points?userId=&page=0&size=20`은 20 rps에서도 실패율 97.42%, p95 5.04초, p99 7.06초, dropped iterations 146건, exit 99로 종료됐다. 같은 실행 직후 `pg_stat_statements`에는 page query 5,669회 평균 442.29ms, count query 5,669회 평균 419.99ms가 남았다.

따라서 Phase 7에서 비교해야 할 기준선은 "page500이 page0보다 얼마나 더 느린가"가 아니라 "no-index + `Page<T>` 기반 points endpoint는 page0 요청만으로도 커넥션과 요청 처리가 포화된다"이다. page500 정밀 비교는 인덱스, cursor, count 제거 같은 개선 후 시스템이 정상 응답 상태를 회복한 뒤 다시 측정해야 한다.

다른 시나리오의 기준선도 확보됐다. `products`는 50 rps에서 exit 0, 실패율 0.013%, p95 621.91ms로 응답은 유지했지만 `product(category_id, status)` 필터가 `Seq Scan`으로 동작했다. `orders`는 50 rps에서 `data_received=0`, 실패율 100%, exit 99였고, 동시에 `order_item` 반복 조회가 12,312회 발생해 N+1이 애플리케이션 응답 실패로 이어지는 기준선을 남겼다.

## 결과 타당성 판단

현재 결과는 목적별로 사용 가능 여부가 다르다.

| 목적 | 판단 | 이유 |
|---|---|---|
| Phase 2 product index 비교 | 사용 가능 | `products` 50 rps 실행은 measurement, k6 summary, exit status, run window, pg_stat, EXPLAIN이 서로 맞다. |
| Phase 3 orders N+1 비교 | 제한적으로 사용 가능 | HTTP latency 비교값은 아니지만, N+1 반복 SQL과 앱 응답 실패 기준선으로 사용할 수 있다. |
| Phase 7 points 운영 한계 비교 | 사용 가능 | fixed page0 rps20 실행이 no-index `point_history`의 page0 운영 한계를 보여준다. |
| Phase 7 page0 vs page500 정밀 비교 | 보류 | 기존 page0/page500 50 rps 실행은 실제 preset이 weighted page였고, 응답 수신도 0B라 비교값으로 사용할 수 없다. |

사용 가능한 근거:

| 근거 | 판단 |
|---|---|
| DB 상태 snapshot | row count, 제약조건, 인덱스 상태가 확인되어 결과 해석에 사용할 수 있다. |
| `products/products-baseline` | measurement, k6 summary, exit status, run window, pg_stat가 있어 Phase 2 기준선으로 사용할 수 있다. |
| `orders/orders-baseline` | HTTP 성능 비교값으로는 부적합하지만, N+1이 앱 응답 실패로 이어진 evidence로 사용할 수 있다. |
| `rps20-test/points-page0-rps20` | fixed page0 preset, k6 summary, run window, exit status, `point_history` pg_stat가 있어 points 운영 한계 근거로 사용할 수 있다. |

사용하지 않는 근거:

| 근거 | 제외 이유 |
|---|---|
| `points/points-page0`, `points/points-page500`의 50 rps 결과 | measurement의 `preset`이 둘 다 `baseline`이고 `page=weighted`다. 조건 이름은 page0/page500이지만 실제 실행 조건은 정밀 page 비교가 아니다. |
| 위 두 points 50 rps k6 p95/p99 | `http_req_failed=1`, `data_received=0`이라 HTTP 응답 성능값이 아니라 앱 응답 실패 신호다. |
| `points/page0`, `points/page500` pg_stat 파일 | 상위 쿼리가 `order_item`, `product_image`라 points SQL snapshot으로 보기 어렵다. |

## 데이터와 테이블 상태

측정 전후 row count와 인덱스 상태는 동일했다.

| Table | Rows | 인덱스 상태 |
|---|---:|---|
| `product` | 100,000 | `product_pkey`만 있음 |
| `orders` | 500,000 | `orders_pkey`만 있음 |
| `order_item` | 1,000,000 | `order_item_pkey`만 있음 |
| `point_history` | 2,000,000 | `point_history_pkey`만 있음 |
| `product_sku` | 300,000 | `product_sku_pkey`, `product_sku_sku_code_key` |
| `product_image` | 200,000 | `product_image_pkey`만 있음 |
| `users` | 10,000 | `users_pkey`, `users_email_key` |

`point_history`에는 `user_id`, `created_at`, `id` 조합을 지원하는 보조 인덱스가 없다. Phase 7 cleanup 이후 기대하는 상태와 동일하게 `idx_point_history_created_id`, `idx_point_history_user_created_id`도 없다.

## 측정 조건

공통 조건:

| Item | Value |
|---|---|
| 측정일 | 2026-06-16 |
| Spring profile | `pool10` |
| k6 mode | `prometheus` |
| k6 timeout | 5초 |
| 주요 label | `phase=phase-01`, `pool=pool10` |

시나리오별 조건:

| Scenario | Preset | Rate | Duration | Target |
|---|---|---:|---|---|
| `products` | `baseline` | 50 rps | 5m | `/api/products?categoryId=1..200&status=...&strategy=baseline` |
| `orders` | `baseline` | 50 rps | 5m | `/api/orders?userId=1..1000&strategy=lazy` |
| `points` failed weighted run | `baseline` | 50 rps | 5m | `/api/points?userId=1..1000&page=weighted&size=20` |
| `products` rps20 control | `baseline-rps20` | 20 rps | 5m | `/api/products?...&strategy=baseline` |
| `points` page0 rps20 | `phase1-points-page0-rps20` | 20 rps | 5m | `/api/points?userId=1..1000&page=0&size=20` |

## k6 결과

| Case | Requests | RPS | Failed | Data received | p95 | p99 | Dropped | Exit | 판단 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|
| `products` 50 rps | 14,935 | 49.78/s | 0.013% | 263.1 MB | 621.91ms | 2.07s | 66 | 0 | Phase 2 기준선 |
| `orders` 50 rps | 14,805 | 48.53/s | 100.00% | 0 B | 5.00s | 5.03s | 196 | 99 | N+1로 인한 응답 실패 evidence |
| `points` weighted 50 rps | 14,704 | 48.21/s | 100.00% | 0 B | 4.93s | 5.00s | 297 | 99 | page0 비교값 아님 |
| `points` weighted 50 rps | 14,353 | 47.06/s | 100.00% | 0 B | 4.94s | 5.01s | 648 | 99 | page500 비교값 아님 |
| `products` 20 rps | 6,000 | 20.00/s | 0.00% | 105.1 MB | 104.56ms | 1.34s | 0 | 0 | lower-load control |
| `points page0` 20 rps | 5,854 | 19.19/s | 97.42% | 514.9 KB | 5.04s | 7.06s | 146 | 99 | points 운영 한계 근거 |

`exit 99`는 k6 threshold 실패다. 이 보고서에서는 `data_received=0`이고 실패율이 100%인 실행을 HTTP 성능 비교값으로 쓰지 않는다. 그런 실행은 "시스템이 요청에 정상 응답하지 못했다"는 evidence로만 분류한다.

## Scenario 해석

### Products baseline

`products`는 50 rps에서 대부분 응답했다. k6 기준 실패율은 0.013%, p95는 621.91ms, p99는 2.07초였다. 하지만 SQL은 효율적이지 않다.

`EXPLAIN ANALYZE`는 `product` 테이블에서 `Seq Scan`을 선택했고, `category_id = 1 and status = 'ON_SALE'` 조건에서 397건을 반환하기 위해 99,603건을 filter로 제거했다. 단건 실행 시간은 67.707ms였다. 부하 실행 직후 `pg_stat_statements`에서는 같은 필터 조회가 14,935회 호출됐고 평균 21.45ms, total 320,401.33ms를 사용했다.

해석은 명확하다. Phase 1의 상품 검색은 응답은 유지하지만, 인덱스 없는 필터 조회가 반복될수록 DB 시간을 누적한다. Phase 2에서는 `(category_id, status)` 계열 인덱스를 적용한 뒤 Seq Scan 제거, 평균 SQL 시간, k6 p95/p99를 비교하면 된다.

### Orders baseline

`orders`는 50 rps에서 실패율 100%, `data_received=0`, exit 99로 끝났다. 이 실행의 p95는 5초 timeout 근처라서 사용자 응답 성능 비교값으로 쓰기 어렵다. 대신 N+1이 실제 DB 작업으로 폭증했다는 근거는 남아 있다.

`pg_stat_statements`에서 `order_item where order_id = ?`는 12,312회 호출됐고 평균 139.78ms, total 1,721,017.69ms를 사용했다. `product_image where product_id = ?`도 24,532회 호출됐고 total 884,785.41ms를 사용했다. `EXPLAIN ANALYZE`는 `orders where user_id = 950`이 `Parallel Seq Scan`으로 414건을 찾고 138.328ms를 사용했으며, `order_item` 단건 조회도 `Parallel Seq Scan`으로 1,000,000건 규모 테이블을 반복 스캔했다.

Phase 3에서는 이 기준선을 "50 rps에서 정상 응답이 없었다"와 "반복 SQL 호출 수가 요청 수보다 훨씬 많았다"로 비교해야 한다. 단순히 p95가 몇 ms에서 몇 ms로 줄었다고 쓰기 전에, 응답 성공률과 `order_item` 호출 수가 먼저 회복되는지 확인해야 한다.

### Points page0 baseline

`points`는 Phase 1의 가장 중요한 결론이다. API smoke에서는 `userId=5714&page=0&size=20` 요청이 20건을 반환했고, 같은 사용자의 `page=500` 요청은 빈 content와 `totalElements=25`를 반환했다. 즉 기능 자체는 동작한다.

하지만 부하에서는 page0부터 한계에 도달했다. fixed page0 preset으로 20 rps를 5분 실행했을 때 요청은 5,854건, 실패율은 97.42%, p95는 5.04초, p99는 7.06초, dropped iterations는 146건이었다. 이 실행은 `data_received=514.9 KB`라서 완전한 0 byte 실행은 아니지만, 대부분의 요청이 timeout 또는 failed check로 기록됐다.

원인은 SQL과 테이블 상태로 설명된다. `point_history`는 2,000,000건이고 `point_history_pkey` 외 보조 인덱스가 없다. page0 단건 `EXPLAIN ANALYZE`도 `Parallel Seq Scan on point_history`를 선택했고, worker별로 약 666,658건을 filter로 제거했다. page0 조회는 75.603ms, count query는 40.642ms였다.

부하 상태에서는 비용이 더 커졌다. rps20 page0 실행 직후 `pg_stat_statements`에서 page query는 5,669회 평균 442.29ms, total 2,507,359.90ms였고, count query는 5,669회 평균 419.99ms, total 2,380,922.59ms였다. `Page<T>` 응답을 만들기 위해 content query와 count query가 함께 실행되며, 둘 다 보조 인덱스 없이 `point_history`를 반복 스캔한다. 이번 보고서는 Hikari 시계열을 근거로 쓰지 않지만, k6 실패율과 SQL 실행 시간이 같은 방향을 가리키므로 no-index page0 조회가 앱 응답 실패의 핵심 원인이라고 볼 수 있다.

따라서 Phase 7의 개선 목표는 deep offset만 제거하는 것이 아니다. 먼저 `user_id + created_at + id` 접근 패턴을 지원하는 인덱스, cursor 기반 조회, count query 제거 또는 분리 중 무엇이 응답 성공률을 회복시키는지 검증해야 한다.

## 다음 Phase 비교 기준

| 다음 Phase | Phase 1 기준선 | 비교해야 할 값 |
|---|---|---|
| Phase 2 Index | `products` 50 rps: 실패율 0.013%, p95 621.91ms, product filter SQL mean 21.45ms, `Seq Scan` | Index Scan 전환, rows removed 감소, SQL mean/total 감소, k6 p95/p99 감소 |
| Phase 3 N+1 | `orders` 50 rps: 실패율 100%, `data_received=0`, `order_item` 12,312 calls | 응답 성공률 회복, `order_item` 반복 호출 제거, 반복 SQL total time 감소 |
| Phase 7 Pagination | `points page0` 20 rps: 실패율 97.42%, p95 5.04s, page query mean 442.29ms, count mean 419.99ms | page0 정상 응답 회복, count query 제거/완화, cursor/index 적용 후 page500 비교 |

다음 k6 실행 전에는 각 케이스 사이에 Spring 재시작 또는 충분한 cooldown을 둬야 한다. `pg_stat_statements_reset()`만으로는 이전 부하에서 남은 애플리케이션 처리 지연이나 DB active query 잔류를 정리하지 못한다.

## 한계와 보완사항

- 이번 보고서는 `points page0`부터 운영 한계에 도달했다는 결론을 쓴다. `page0보다 page500이 몇 배 느리다`는 결론은 쓰지 않는다.
- `points/points-page0`, `points/points-page500` 50 rps evidence는 condition 이름과 실제 preset이 맞지 않는다. 두 실행 모두 `preset=baseline`, `page=weighted`로 기록되어 fixed page 비교 근거가 아니다.
- `orders`와 50 rps `points` 실행은 `data_received=0`이므로 HTTP p95/p99를 성능 비교 수치로 쓰지 않는다. 이 값들은 timeout/응답 실패 상태를 보여주는 보조 신호다.
- 더 정밀한 Phase 7 비교가 필요하면 Spring을 재시작한 뒤 `phase1-points-page0-rps20`, `phase1-points-page500-rps20` 또는 더 낮은 rps를 같은 조건으로 재실행하고, 각 실행의 measurement와 pg_stat를 분리해서 저장해야 한다.

## 주요 근거

- Evidence index: [docs/evidence/phase-01/README.md](../../evidence/phase-01/README.md)
- DB before snapshot: [before-measurement-state.txt](../../evidence/phase-01/db/before-measurement-state.txt)
- DB after snapshot: [after-measurement-state.txt](../../evidence/phase-01/db/after-measurement-state.txt)
- Products k6: [k6-summary.json](../../evidence/phase-01/products/products-baseline/k6-summary.json), [measurement.json](../../evidence/phase-01/products/products-baseline/measurement.json), [pg-stat-statements.txt](../../evidence/phase-01/products/products-baseline/pg-stat-statements.txt)
- Orders k6: [k6-summary.json](../../evidence/phase-01/orders/orders-baseline/k6-summary.json), [measurement.json](../../evidence/phase-01/orders/orders-baseline/measurement.json), [pg-stat-statements.txt](../../evidence/phase-01/orders/orders-baseline/pg-stat-statements.txt)
- Points page0 rps20: [k6-summary.json](../../evidence/phase-01/rps20-test/points-page0-rps20/k6-summary.json), [measurement.json](../../evidence/phase-01/rps20-test/points-page0-rps20/measurement.json), [pg-stat-statements.txt](../../evidence/phase-01/rps20-test/points-page0-rps20/pg-stat-statements.txt)
- SQL explain: [products-baseline-explain.txt](../../evidence/phase-01/explain/products-baseline-explain.txt), [orders-lazy-explain.txt](../../evidence/phase-01/explain/orders-lazy-explain.txt), [points-offset-explain.txt](../../evidence/phase-01/explain/points-offset-explain.txt)
