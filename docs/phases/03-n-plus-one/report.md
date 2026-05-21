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
