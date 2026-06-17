# Phase 3 범위

## 목표

Phase 3의 목표는 주문 목록 화면의 상품 썸네일 조회를 기준으로 Lazy naive, Fetch Join, BatchSize, EntityGraph 전략의 SQL count와 부하 지표 차이를 기록하는 것이다.

## 대상 API와 조회 경로

| 대상 | 값 |
|---|---|
| API | `GET /api/orders?userId=&strategy=` |
| Strategies | `lazy`, `fetch-join`, `batch-size`, `entity-graph` |
| Entity path | `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` |
| Snapshot fields | `OrderItem.productName`, `optionInfo`, `quantity`, `unitPrice` |
| Thumbnail source | `Product.images`에서 대표 이미지 |

## 제외 범위

- 기본 fetch type을 `EAGER`로 고정하지 않는다.
- `order_item.product_name`으로 `Product`를 역조회하지 않는다.
- QueryDSL DTO projection 최적화는 Phase 5로 남긴다.
- 동시성, 격리 수준, 재고 정합성은 Phase 4로 남긴다.
- 인덱스 실행계획 전환 자체를 Phase 3의 핵심 목표로 삼지 않는다.

## 완료 조건

- [x] Lazy naive에서 `1 + N + 3M` 형태의 반복 쿼리가 재현됐다.
- [x] Fetch Join과 BatchSize 전략의 SQL count, `pg_stat_statements`, k6 결과를 저장했다.
- [x] EntityGraph 전략의 결과를 저장하거나 제외 사유를 report에 기록했다.
- [x] 대표 SQL의 `EXPLAIN (ANALYZE, BUFFERS)`를 저장했다.
- [x] Grafana Phase 3 evidence에서 latency, failure, dropped iterations, Hikari pressure를 비교했다.
- [x] Phase 4로 넘길 동시성/정합성 질문을 report에 기록했다.
