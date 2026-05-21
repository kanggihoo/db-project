# Phase 3 결과 보고서

## 결론

Phase 3는 주문 목록 썸네일 조회에서 Lazy naive가 요청당 SQL 수를 크게 늘리는 것을 확인했다. 기본 FK 조회 인덱스를 적용한 뒤에도 Lazy는 평균 약 2,454 SQL calls/request를 만들었고, `rate=1` 조건에서 k6 p95가 3.49초까지 올라갔다.

Fetch Join과 EntityGraph는 `Orders -> OrderItems -> ProductSku -> Product` 경로의 반복 select를 줄였지만, `ProductImages` 조회가 반복 select로 남아 평균 약 698~699 SQL calls/request를 기록했다. BatchSize는 Lazy 접근을 유지하면서 연관 조회를 `IN (...)` 쿼리로 묶어 평균 약 27.2 SQL calls/request까지 낮췄고, 이번 Phase 3 evidence에서는 가장 낮은 p95를 보였다.

따라서 Phase 3의 결론은 "모든 연관을 무조건 fetch join한다"가 아니라, 조회 화면의 응답 조립 경로를 기준으로 반복 접근이 발생하는 지점을 확인하고 페이징, collection 중복, row multiplication 위험을 함께 고려해 로딩 전략을 선택해야 한다는 것이다.

## 측정 조건

| 항목 | 값 |
|---|---|
| phase | `phase-03` |
| scenario | `orders` |
| preset | `baseline` |
| pool | `pool10` |
| API | `GET /api/orders?userId=&strategy=` |
| entity path | `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` |
| baseUrl | `http://host.docker.internal:8080` |
| rate | `1` iteration/s |
| duration | `5m` |
| preAllocatedVUs | `20` |
| maxVUs | `100` |
| timeout | `30s` |
| user range | `1..1000` |
| page size | `20` |

SQL calls/request는 `pg_stat_statements`의 반복 app query calls를 k6 `http_reqs`로 나눠 계산했다. `VACUUM ANALYZE`와 대표 `EXPLAIN` 수집 과정에서 생긴 one-off helper query는 비교값에서 제외했다.

단일 shape snapshot은 모든 strategy에서 동일하게 `user_id=100` 기준 `orders=381`, `order_item=762`, `distinct_sku=761`, `distinct_product=760`이었다.

## 지표 산출 방식

이번 report의 지표는 단일 파일에서 나온 값이 아니라 k6, `pg_stat_statements`, Grafana를 역할별로 나눠 사용했다.

| 지표 | 출처 | 산출 방식 | 의미 |
|---|---|---|---|
| `SQL calls/request` | `docs/evidence/phase-03/orders/<strategy>/pg-stat-statements.txt`, `k6-summary.txt` | 비교 대상 app query의 `calls` 합계를 k6 `http_reqs`로 나눈 평균값 | API 요청 1번을 만들기 위해 DB SQL이 평균 몇 번 실행됐는지 |
| `k6 p95` | `k6-summary.txt` 마지막 summary의 `http_req_duration p(95)` | k6가 측정한 HTTP 요청 duration의 95 percentile | 사용자 요청 latency 상위 5% 경계 |
| `failed rate` | `k6-summary.txt`의 `http_req_failed` | 실패 HTTP 요청 수 / 전체 HTTP 요청 수 | timeout, connection failure, 4xx/5xx 등 k6 기준 실패율 |
| `dropped` | `k6-summary.txt`, Grafana `Dropped Iterations` | k6가 목표 arrival rate를 맞추지 못해 시작하지 못한 iteration 수 | 부하 발생기 관점의 처리 지연 신호 |
| `Hikari pending max` | Grafana `Hikari Pending Max`, `Pending Threads` | 측정 window에서 connection 획득 대기 thread의 최대값 | 애플리케이션 connection pool 병목 여부 |
| Grafana p95/p99 | Grafana Run Summary, k6 Load panels | Prometheus에 적재된 k6 metric의 time-series 집계 | k6 summary 값이 시간 흐름상 어떻게 나타났는지 확인 |

`SQL calls/request`는 "단일 요청이 항상 정확히 이만큼 SQL을 실행한다"는 뜻이 아니다. k6가 5분 동안 여러 `userId`로 실행한 전체 SQL calls를 전체 HTTP 요청 수로 나눈 평균값이다. 같은 preset, 같은 API, 같은 데이터 범위에서 측정했기 때문에 strategy 간 비교 지표로 사용한다.

## SQL calls/request 계산 상세

`pg_stat_statements`에는 helper query도 함께 들어갈 수 있으므로, 아래 계산에서는 주문 API 응답 조립 과정에서 반복 실행된 app query만 포함했다. 예를 들어 `VACUUM ANALYZE`, 대표 `EXPLAIN` 수집용 단발 query는 제외했다.

| Strategy | 포함한 app query calls | k6 `http_reqs` | 계산식 | SQL calls/request |
|---|---:|---:|---|---:|
| lazy | 738,528 | 301 | `738,528 / 301` | 약 2,454 |
| fetch-join | 210,519 | 301 | `210,519 / 301` | 약 699 |
| batch-size | 8,165 | 300 | `8,165 / 300` | 약 27.2 |
| entity-graph | 210,108 | 301 | `210,108 / 301` | 약 698 |

Lazy의 738,528 calls는 `orders` 301회, `order_item` 105,695회, `product_sku` 211,164회, `product` 210,684회, `product_image` 210,684회를 합산한 값이다. 요청 301회 동안 하위 연관 조회가 수십만 번 반복되었으므로 N+1이 실제 부하에서 재현됐다고 판단한다.

Fetch Join은 main query가 301회로 줄었지만 `product_image where product_id=$1`가 210,218회 남아 있다. 따라서 SQL calls/request가 Lazy보다는 크게 줄었지만 약 699에서 멈췄다.

BatchSize는 `order_item`, `product_sku`, `product`, `product_image` 조회가 `= any ($1)` batch query로 묶였다. 같은 5분 구간에서 포함한 app query calls가 8,165회로 줄어 SQL calls/request가 약 27.2까지 낮아졌다.

EntityGraph는 main path를 left join으로 가져왔지만 `ProductImages` 반복 조회가 남았다. 그래서 Fetch Join과 유사하게 약 698 SQL calls/request를 기록했다.

## k6 지표 계산 상세

`k6-summary.txt`는 progress line이 많기 때문에 각 파일의 마지막 summary 영역만 사용했다.

| Strategy | k6 `http_reqs` | `http_req_duration p(95)` | `http_req_failed` | `dropped_iterations` | 해석 |
|---|---:|---:|---:|---:|---|
| lazy | 301 | 3.49s | 0.00% | 0 | 실패는 없지만 같은 rate에서 latency가 가장 큼 |
| fetch-join | 301 | 1.06s | 0.00% | 0 | Lazy 대비 p95가 약 69.6% 감소 |
| batch-size | 300 | 155.85ms | 0.00% | 0 | Lazy 대비 p95가 약 95.5% 감소 |
| entity-graph | 301 | 1.07s | 0.00% | 0 | Fetch Join과 거의 같은 latency 구간 |

`failed rate=0.00%`와 `dropped=0`은 최종 `rate=1` 비교 구간이 장애 상황이 아니라 정상 처리 상태였다는 뜻이다. 따라서 이 구간에서는 "어느 전략이 서버를 죽였는가"가 아니라 "같은 요청을 처리할 때 어떤 SQL shape가 더 적은 round-trip과 낮은 latency를 만드는가"를 비교한다.

## 측정 조정 기록

초기 Lazy baseline은 `orders baseline` preset의 `rate=50`, `timeout=5s` 조건으로 실행했다. 이 실행에서는 k6가 초당 약 50회 요청을 시작했고, Lazy N+1 경로가 빠르게 누적되면서 서버가 정상적인 HTTP 응답을 거의 반환하지 못했다. k6 summary에는 `http_req_failed=100%`, `data_received=0 B`, 다수의 `connect: connection refused`가 기록되었고, host 측 8080 포트에는 `CLOSE_WAIT`/`FIN_WAIT_2` 연결이 대량으로 누적되었다.

이 결과는 Lazy 전략의 병목을 보여주는 붕괴 관찰로는 의미가 있지만, Fetch Join, BatchSize, EntityGraph와 같은 조건에서 p95나 SQL shape를 비교하기에는 적합하지 않다. 따라서 최종 비교표에는 50 rps 붕괴 실행 값을 섞지 않는다.

기본 FK 조회 인덱스를 준비한 뒤에도 Lazy는 `rate=5`, `timeout=30s` 조건에서 안정 측정 상태에 도달하지 못했다. Grafana와 k6 summary 기준으로 p95는 약 31.9초, p99는 약 32.0초였고, 실제 처리량은 목표 5 req/s보다 낮은 약 3.03 req/s에 머물렀다. Error Rate는 약 50%, Checks Success는 약 23.4%, Dropped Iterations는 428까지 증가했으며, Hikari Pending Max는 100까지 상승했다.

따라서 `rate=5` 결과는 Lazy의 capacity collapse evidence로 분리하고, strategy 간 p95와 SQL shape를 비교하기 위한 최종 Lazy 측정은 `rate=1`, `timeout=30s`로 낮춰 진행했다.

## 제외된 초기 실행

초기 Lazy 실행 일부는 최종 strategy 비교표에서 제외한다. 이 실행 당시 DB에는 `orders(user_id)`, `order_item(order_id)`, `product_image(product_id)` 기본 FK 조회 인덱스가 없었다. `EXPLAIN`은 `order_item WHERE order_id = 100` 대표 쿼리가 `Parallel Seq Scan`으로 실행되어 실제 2건을 반환하기 위해 worker별 약 333,333건을 제거했음을 보여줬다.

따라서 이 실행은 로딩 전략만의 차이가 아니라 `N+1 * FK 인덱스 부재`를 측정한 붕괴 실행이다. 이 결과는 환경 검증과 문제 분리 기록으로 보관하되, 최종 Phase 3 strategy 비교표의 수치로 사용하지 않는다.

## 기본 인덱스 전제

Phase 3의 목적은 FK 조회 인덱스 부재를 측정하는 것이 아니라 `Lazy`, `Fetch Join`, `BatchSize`, `EntityGraph` 로딩 전략 차이를 비교하는 것이다. 따라서 비교 evidence를 수집하기 전에 관계 조회에 필요한 기본 인덱스를 준비했다.

```sql
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_product_image_product_id ON product_image (product_id);
```

최종 Lazy `EXPLAIN`은 `order_item(order_id)` 조회가 `Index Scan using idx_order_item_order_id`로 실행됨을 확인했다.

## 전략별 비교

| Strategy | SQL calls/request | k6 p95 | failed rate | dropped | Hikari pending max | Main SQL shape | Notes |
|---|---:|---:|---:|---:|---:|---|---|
| lazy | 약 2,454 | 3.49s | 0.00% | 0 | 0 | repeated select | `OrderItem`, `ProductSku`, `Product`, `ProductImages` 반복 조회 |
| fetch-join | 약 699 | 1.06s | 0.00% | 0 | 0 | join fetch | main path join fetch, `ProductImages` 반복 select 잔존 |
| batch-size | 약 27.2 | 155.85ms | 0.00% | 0 | 0 | `IN (...)` | 연관 조회가 `ANY ($1)` batch query로 묶임 |
| entity-graph | 약 698 | 1.07s | 0.00% | 0 | 0 | graph loading | fetch-join과 유사한 main path join, `ProductImages` 반복 select 잔존 |

## 측정 지표 기반 분석

Lazy를 기준으로 보면 Fetch Join과 EntityGraph는 SQL calls/request를 약 71.5% 줄였고, k6 p95도 약 69% 낮췄다. 두 전략은 main path의 반복 select를 join query로 바꾸는 효과가 있었지만, `ProductImages`는 여전히 product별 lazy select로 남았기 때문에 SQL calls/request가 약 700 수준에서 멈췄다.

BatchSize는 SQL calls/request를 약 98.9% 줄였고, k6 p95도 Lazy 대비 약 95.5% 낮췄다. `pg_stat_statements`에서 product, sku, image 조회가 개별 `id=$1` lookup이 아니라 `ANY ($1)` batch query로 바뀐 것이 latency 감소와 직접 연결된다. 이번 데이터 shape에서는 주문 381건, order item 762건, distinct product 760건이 한 요청에 걸려 있으므로, 개별 product/image 접근을 batch로 묶는 효과가 가장 크게 나타났다.

모든 최종 `rate=1` 실행은 failure `0.00%`, dropped iterations `0`, Hikari pending max `0`이었다. 따라서 최종 비교 구간은 장애 재현 구간이 아니라 정상 처리 상태에서 로딩 전략별 SQL shape와 latency를 비교한 구간이다. 반대로 Lazy `rate=5` 실행은 p95 약 31.9초, Error Rate 약 50%, Dropped Iterations 428, Hikari Pending Max 100을 기록했으므로 capacity collapse evidence로 별도 분리한다.

## Grafana 관측 요약

Grafana는 k6 summary와 같은 구간을 time-series로 확인하기 위한 보조 evidence다. Actual RPS panel의 마지막 값은 window padding과 마지막 scrape 시점의 영향을 받아 k6 summary의 전체 평균보다 낮게 보일 수 있다. 최종 처리량 판단은 k6 summary의 `http_reqs`를 우선하고, Grafana는 latency 흐름과 runtime pressure 확인에 사용한다.

| Strategy | Grafana p95 | Grafana p99 | Actual RPS panel | Error Rate | Checks Success | PG Connections Used | Hikari Pending Max | Interpretation |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| lazy | 3.76s | 3.95s | 0.820 req/s | 0% | 100% | 10% | 0 | 실패는 없지만 latency가 가장 높고, `/api/orders` p95도 약 3.70s로 유지됨 |
| fetch-join | 1.06s | 1.08s | 0.821 req/s | 0% | 100% | 8% | 0 | main path join으로 latency가 낮아졌지만 image 반복 select는 남음 |
| batch-size | 142ms | 169ms | 0.876 req/s | 0% | 100% | 8% | 0 | p95/p99가 가장 낮고 runtime pressure도 낮음 |
| entity-graph | 1.11s | 1.17s | 0.850 req/s | 0% | 100% | 8% | 0 | Fetch Join과 비슷한 latency와 connection profile |

Grafana의 Hikari Pool 패널에서는 최종 네 전략 모두 pending thread가 0으로 유지됐다. 이는 `rate=1` 비교 구간에서는 커넥션 풀이 병목이 아니라는 뜻이다. PostgreSQL activity와 table access 패널도 seq scan 증가 없이 index access 중심으로 유지되어, 기본 FK 인덱스 적용 후 비교가 진행됐음을 뒷받침한다.

CPU, heap, GC pause도 낮은 범위에서 유지됐다. 따라서 최종 비교의 병목 해석은 CPU saturation이나 GC pressure가 아니라, 한 요청을 만들기 위해 발생한 SQL shape와 round-trip 수 차이에 집중하는 것이 맞다.

## pg_stat_statements 요약

| Strategy | Top query shape | calls | total time |
|---|---|---:|---:|
| lazy | `product_image where product_id=$1` | 210,684 | 7,229.20ms |
| lazy | `product where id=$1` | 210,684 | 6,126.07ms |
| lazy | `product_sku where id=$1` | 211,164 | 5,870.52ms |
| lazy | `order_item where order_id=$1` | 105,695 | 4,621.66ms |
| fetch-join | `product_image where product_id=$1` | 210,218 | 6,956.95ms |
| fetch-join | `select distinct orders... join order_item... product_sku... product` | 301 | 4,052.95ms |
| batch-size | `product_image where product_id = any ($1)` | 2,222 | 1,316.79ms |
| batch-size | `order_item where order_id = any ($1)` | 1,199 | 1,189.55ms |
| batch-size | `product where id = any ($1)` | 2,222 | 1,178.52ms |
| batch-size | `product_sku where id = any ($1)` | 2,222 | 859.23ms |
| entity-graph | `product_image where product_id=$1` | 209,807 | 6,579.85ms |
| entity-graph | `select orders... left join order_item... product_sku... product` | 301 | 4,231.07ms |

## 해석

### Lazy naive

Lazy naive는 기본 `LAZY` 매핑 자체가 문제가 아니라, 주문 목록 응답 조립 과정에서 연관 객체를 순차 접근하면서 요청당 SQL 수가 증가한다는 점을 보여준다. 최종 `rate=1` 실행에서는 실패 없이 완료됐지만, 평균 약 2,454 SQL calls/request와 3.49초 p95를 기록했다. 같은 인덱스 조건에서도 `rate=5`에서는 pending thread가 쌓이며 붕괴했으므로, Lazy naive는 이 화면의 기본 전략으로 적합하지 않다.

### Fetch Join

Fetch Join은 `Orders -> OrderItems -> ProductSku -> Product` 경로를 한 번에 당겨 반복 select를 줄인다. 실제로 main query는 301회만 실행됐고 p95도 1.06초까지 낮아졌다. 다만 `Product.images`는 별도 collection이기 때문에 반복 select가 남았고, `product_image where product_id=$1`가 210,218회 실행됐다. collection fetch join은 row duplication을 만들 수 있고, 페이징과 함께 사용할 때 Hibernate 경고와 메모리 페이징 위험이 있으므로 적용 범위를 제한해야 한다.

### BatchSize

BatchSize는 Lazy 접근을 유지하면서 개별 select를 `IN (...)`/`ANY ($1)` 쿼리로 묶는다. 쿼리 수를 1회로 만들지는 않지만, 이번 주문 목록처럼 페이징과 다중 연관이 섞이는 조회에서 더 안전한 선택지가 될 수 있다. Phase 3 evidence에서는 평균 SQL calls/request가 약 27.2로 줄었고, k6 p95도 155.85ms로 가장 낮았다.

### EntityGraph

EntityGraph는 repository method에 조회 시점 로딩 범위를 선언한다. 이번 evidence에서는 Fetch Join과 유사하게 main path를 join으로 가져왔고 p95도 1.07초로 낮아졌다. 그러나 `ProductImages` 반복 select가 남아 SQL calls/request는 Fetch Join과 비슷했다. 복잡한 조건, 정렬, 페이징이 필요해질수록 EntityGraph만으로 조회 의도를 표현하기보다 JPQL 또는 QueryDSL 쪽이 더 명확할 수 있다.

## Phase 4 Handoff

Phase 3는 조회 로딩 전략 문제를 다뤘다. 다음 질문은 조회가 아니라 동시에 여러 사용자가 주문/재고를 변경할 때 데이터 정합성이 어떻게 유지되는가다. Phase 4는 트랜잭션 격리 수준과 재고 정합성 실험으로 넘어간다.
