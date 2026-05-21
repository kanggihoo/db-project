# 이커머스 DB 최적화 학습 로드맵

## Phase 3. N+1 + 로딩 전략 최적화

> "주문 목록 화면에서 왜 연관 조회가 폭증하고, 로딩 전략에 따라 SQL 모양이 어떻게 달라지는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

Phase 2는 `Product` 필터 쿼리의 단일 SQL 실행계획과 인덱스 효과를 확인했다. Phase 3는 인덱스만으로는 해결되지 않는 **요청당 쿼리 수 증가 문제**를 다룬다.

대상 화면은 주문 목록이다. 주문 목록은 주문 당시 상품명, 옵션, 가격은 **Order Item Snapshot**을 사용하지만, 화면 썸네일은 현재 상품의 대표 이미지를 보여준다는 요구사항을 둔다. 따라서 주문 목록 조회는 다음 연관 경로를 접근한다.

```text
Orders
  -> OrderItems
  -> ProductSku
  -> Product
  -> ProductImages
```

이 경로는 실제 주문 목록 UI에서 흔한 "주문 상품 + 썸네일" 표시를 재현하기 위한 실험용 조회 경로다.

### N+1 대표 시나리오

```text
주문 목록 100건 조회
  -> Orders 1번 쿼리
  -> OrderItems N번 쿼리
  -> ProductSku M번 쿼리
  -> Product M번 쿼리
  -> ProductImages M번 쿼리
= 1 + N + 3M 쿼리 발생
```

`N`은 조회된 주문 수이고, `M`은 조회된 주문 상품 수다. 주문당 평균 2개 상품이면 주문 100건에서 대략 `1 + 100 + 600 = 701`회 형태의 쿼리 폭증이 발생할 수 있다.

정확한 FK 경로는 다음과 같다.

```text
orders.id
  -> order_item.order_id
  -> order_item.sku_id
  -> product_sku.id
  -> product_sku.product_id
  -> product.id
  -> product_image.product_id
```

`order_item.product_name`은 주문 당시 상품명 스냅샷이므로 `Product`를 역조회하는 키로 사용하지 않는다.

### 실험 API 전략

동일한 주문 목록 응답을 유지하고 `strategy` 파라미터로 로딩 전략만 바꿔 비교한다.

```text
GET /api/orders?userId=123&strategy=lazy
GET /api/orders?userId=123&strategy=fetch-join
GET /api/orders?userId=123&strategy=batch-size
GET /api/orders?userId=123&strategy=entity-graph
```

기본 엔티티 매핑은 `LAZY`를 유지한다. `FetchType.EAGER`를 기본 매핑에 고정하지 않고, 조회 목적별로 Fetch Join, EntityGraph, BatchSize를 적용한다.

### 단계별 해결 비교

| # | 전략 | 구현 방식 | SQL 모양 | 확인할 것 |
|---|---|---|---|---|
| 1 | Lazy naive | 기본 `LAZY` 연관 접근 | 개별 select 반복 | `1 + N + 3M` 쿼리 폭증 |
| 2 | Fetch Join | JPQL `join fetch` | 큰 join 쿼리 | 쿼리 수 감소, row 중복, 컬렉션 fetch 한계 |
| 3 | BatchSize | `default_batch_fetch_size` 또는 `@BatchSize` | `where id in (...)` 묶음 조회 | 페이징/다중 연관에서 안정적인 완화 |
| 4 | EntityGraph | Repository 메서드의 `@EntityGraph` | 조회 시점 로딩 계획 | JPQL 변경 없이 필요한 연관만 로딩 |

Phase 3 완료를 위해서는 Lazy naive를 재현하고, Fetch Join과 BatchSize를 최소 비교 대상으로 삼는다. EntityGraph는 선택 비교 대상으로 둔다.

### 핵심 함정: Fetch Join + 컬렉션 + 페이징

```java
@Query("""
    select distinct o
    from Orders o
    join fetch o.orderItems oi
    join fetch oi.productSku sku
    join fetch sku.product p
    where o.userId = :userId
""")
List<Orders> findByUserIdWithFetchJoin(Long userId);
```

Fetch Join은 단일 조회에서 N+1을 크게 줄일 수 있지만, 컬렉션 fetch join은 row 수를 늘리고 페이징과 함께 사용할 때 Hibernate 경고와 메모리 페이징 위험을 만들 수 있다. `Product.images`처럼 추가 컬렉션까지 동시에 fetch join하면 row explosion이 더 커질 수 있으므로, 대표 이미지 조회는 별도 batch 또는 제한된 조회로 분리할 수 있다.

BatchSize는 Lazy 접근을 유지하면서 다음과 같은 SQL로 반복 조회를 줄인다.

```sql
select *
from order_item
where order_id in (?, ?, ?, ...);

select *
from product_sku
where id in (?, ?, ?, ...);
```

### Evidence 수집 방식

Phase 3의 핵심 Evidence는 실행계획 전환보다 **요청당 SQL 수와 쿼리 shape별 호출 수**다.

```text
docs/evidence/phase-03/orders/lazy/
docs/evidence/phase-03/orders/fetch-join/
docs/evidence/phase-03/orders/batch-size/
docs/evidence/phase-03/orders/entity-graph/
```

각 전략 디렉토리에는 같은 종류의 파일을 둔다.

```text
k6-summary.txt
pg-stat-statements.txt
sql-count.txt
representative-sql.txt
explain.txt
grafana-screenshot.png
```

`sql-count.txt`는 단일 요청에서 발생한 SQL 수를 기록한다.

```text
Strategy: lazy
Request: GET /api/orders?userId=123&strategy=lazy

Observed SQL count:
- orders by user_id: 1
- order_item by order_id: N
- product_sku by id: M
- product by id: M
- product_image by product_id: M
- total: 1 + N + 3M
```

`pg-stat-statements.txt`는 전략별 실행 전에 `pg_stat_statements_reset()`을 수행한 뒤 저장해 쿼리 calls, mean time, total time이 섞이지 않게 한다.

### k6와 Grafana로 확인하는 것

k6는 전략별로 분리 실행한다. 각 실행은 같은 Measurement Condition을 유지하고 `strategy`만 바꾼다.

```bash
PHASE=phase-03 STRATEGY=lazy ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=fetch-join ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=batch-size ./k6/run.sh orders smoke prometheus

PHASE=phase-03 STRATEGY=lazy ./k6/run.sh orders baseline prometheus
PHASE=phase-03 STRATEGY=fetch-join ./k6/run.sh orders baseline prometheus
PHASE=phase-03 STRATEGY=batch-size ./k6/run.sh orders baseline prometheus
```

smoke 실행은 SQL shape와 쿼리 수 확인용이고, baseline 실행은 최종 비교 Evidence용이다.

Grafana의 Phase 3 focus row는 Table Scan 전환보다 다음 지표를 중심으로 해석한다.

- Orders API p95/p99 latency by strategy
- request failure rate by strategy
- dropped iterations by strategy
- Hikari active connections
- Hikari pending threads
- Hikari acquire time
- PostgreSQL active sessions

쿼리 원문이나 고카디널리티 요청 값은 Prometheus label로 올리지 않는다. `strategy=lazy|fetch-join|batch-size|entity-graph`는 낮은 카디널리티 라벨로 사용할 수 있다.

### EXPLAIN의 역할

Phase 2에서는 `EXPLAIN (ANALYZE, BUFFERS)`가 핵심 Evidence였지만, Phase 3에서는 보조 Evidence다. 대표 SQL에 대해서만 실행계획을 기록한다.

- Lazy naive의 반복 쿼리: `order_item where order_id = ?`
- Fetch Join의 큰 join 쿼리
- BatchSize의 `IN (...)` 쿼리

EXPLAIN은 "왜 이 쿼리 하나가 빠르거나 느린가"를 설명하고, `sql-count.txt`와 `pg_stat_statements`는 "요청 하나가 쿼리를 얼마나 많이 발생시키는가"를 설명한다.

### 이 Phase에서 얻는 인사이트

- N+1은 Lazy Loading 자체의 문제가 아니라 연관 데이터 접근 패턴과 조회 목적의 문제다.
- 기본 매핑은 `LAZY`로 두고, 조회 API별로 필요한 로딩 전략을 선택하는 것이 안전하다.
- Fetch Join은 쿼리 수를 크게 줄일 수 있지만 컬렉션, row 중복, 페이징 한계가 있다.
- BatchSize는 쿼리를 1회로 만들지는 않지만 `IN` 쿼리로 묶어 실무적인 완화 효과를 낸다.
- EntityGraph는 조회 시점별 로딩 계획을 선언할 수 있지만 복잡한 조건/정렬/페이징에서는 Fetch Join이나 QueryDSL이 더 명확할 수 있다.

### 측정 지표 (회고용)

- 전략별 요청당 SQL 수
- 전략별 `pg_stat_statements.calls`, `mean_exec_time`, `total_exec_time`
- 전략별 k6 p95/p99 응답시간
- 전략별 request failure rate와 dropped iterations
- Hikari active/pending/acquire time
- 대표 SQL의 `EXPLAIN (ANALYZE, BUFFERS)`

### 남은 문제 -> Phase 4로

> "조회 쿼리 수는 줄였는데, 동시에 여러 사용자가 주문/재고를 변경하면 데이터 정합성은 어떻게 되는가?"

Phase 4는 조회 로딩 전략이 아니라 동시성, 격리 수준, 재고 정합성 문제로 넘어간다.

### 완료 조건

- [ ] 주문 목록 화면의 상품 썸네일 요구사항을 기준으로 `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` 조회 경로를 정의했다.
- [ ] 기본 `LAZY` 연관 접근으로 `1 + N + 3M` 형태의 N+1을 재현했다.
- [ ] `strategy` 파라미터로 Lazy, Fetch Join, BatchSize를 분리해 같은 응답을 비교했다.
- [ ] Fetch Join과 BatchSize 중 최소 2개 전략의 SQL count, `pg_stat_statements`, k6 결과를 기록했다.
- [ ] Fetch Join의 컬렉션/페이징 한계 또는 row duplication 위험을 확인했다.
- [ ] 대표 SQL의 `EXPLAIN (ANALYZE, BUFFERS)`를 보조 Evidence로 기록했다.
- [ ] Grafana Phase 3 focus row에서 latency, failure, dropped iterations, Hikari pressure를 비교했다.
- [ ] 동시 요청 시 데이터 일관성 관점의 다음 실험 질문을 Phase 4로 연결했다.

---
