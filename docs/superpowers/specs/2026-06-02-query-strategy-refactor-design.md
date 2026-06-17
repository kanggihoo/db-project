# Query Strategy Refactor Design

## 배경

이 프로젝트는 **Ecommerce DB Optimization Lab**으로, 각 **Learning Phase**에서 하나의 DB 문제를 재현하고 **Evidence**로 비교한다. Phase 3, 5, 7은 모두 "같은 도메인 요구를 여러 조회 방식으로 비교한다"는 공통점을 가진다.

- Phase 3: `Orders -> OrderItem -> ProductSku -> Product` 조회에서 Lazy, Fetch Join, Batch Size, EntityGraph 로딩 전략 비교
- Phase 5: 상품 검색에서 Spring Data JPA entity loading baseline과 QueryDSL DTO projection 비교
- Phase 7: **Point History** 목록에서 Offset/Page와 Cursor pagination 비교

현재 코드는 이 전략 차이를 주로 service 내부 분기로 표현한다.

- `OrderService`는 `OrderLoadingStrategy` enum을 switch해서 private method를 호출한다.
- `ProductService`는 `ProductSearchStrategy` enum을 switch해서 baseline repository와 QueryDSL repository를 고른다.
- `PointService`는 offset/page와 cursor pagination을 같은 service 안의 서로 다른 method로 처리한다.

이 구조는 현재 Phase Evidence를 재현하는 데는 충분하다. 하지만 이후 Phase가 늘어나면 service 파일이 실험 전략 저장소처럼 변하고, 새 실험 방식이 추가될 때 기존 service와 controller가 반복적으로 수정된다.

참고 문서인 `2026-05-30-reservation-strategy-refactor-design.md`는 콘서트 예약 도메인에서 `ReservationService`에 몰린 no-lock, pessimistic, optimistic, atomic 전략을 strategy class와 registry로 분리하는 설계다. 현재 프로젝트에는 reservation 도메인이 없으므로 해당 구조를 그대로 옮기지 않는다. 대신 같은 의도인 "전략별 구현을 새 파일로 추가하고, central switch/hot spot을 줄인다"를 현재 이커머스 조회 실험에 맞춰 적용한다.

## 현재 문제

### 1. Phase 3 Order loading 전략이 `OrderService` switch에 묶여 있다

현재 `OrderService`는 다음 구조를 가진다.

```java
public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategy strategy) {
    return switch (strategy) {
        case LAZY -> getOrdersByUserIdLazy(userId);
        case FETCH_JOIN -> getOrdersByUserIdFetchJoin(userId);
        case BATCH_SIZE -> getOrdersByUserIdLazy(userId);
        case ENTITY_GRAPH -> getOrdersByUserIdEntityGraph(userId);
    };
}
```

새 로딩 전략을 추가하려면 enum, service switch, private method가 함께 바뀐다. Phase 3의 실험 의도는 전략 비교인데, 구현은 하나의 service 파일에 모여 있다.

### 2. Phase 5 Product search 전략이 `ProductService` switch에 묶여 있다

현재 `ProductService`는 `ProductSearchStrategy.BASELINE`과 `ProductSearchStrategy.QUERYDSL`을 switch로 선택한다.

```java
return switch (strategy) {
    case BASELINE -> searchProductsBaseline(categoryId, status);
    case QUERYDSL -> productQueryRepository.searchProducts(categoryId, status);
};
```

QueryDSL 외에 JDBC projection, native SQL, covering-index 전용 projection 같은 실험이 생기면 `ProductService`가 계속 변경된다.

### 3. Phase 7 Point pagination은 전략 차이가 있지만 응답 계약이 다르다

Offset/Page API와 Cursor API는 모두 **Point History** pagination 실험이지만 응답 타입이 다르다.

```text
Offset/Page -> Page<PointHistoryResponse>
Cursor      -> PointHistoryCursorResponse
```

따라서 Phase 3/5와 같은 단일 registry로 억지 통합하면 오히려 타입과 API 의미가 흐려진다. Phase 7은 "공통 strategy registry"가 아니라 offset reader와 cursor reader를 분리해 `PointService`의 책임을 줄이는 방식이 맞다.

### 4. 이후 Phase 11 Concurrency Control에서 같은 문제가 더 커질 수 있다

로드맵상 Phase 11은 재고 차감, 쿠폰 발급, 중복 요청 방지를 대상으로 비관적 락, 낙관적 락, Atomic UPDATE, SERIALIZABLE retry, idempotency를 비교한다. 지금 조회 전략 분리를 정리해두면, Phase 11에서 `StockDeductionStrategy`, `CouponIssueStrategy` 같은 전략 구조를 도입할 때 같은 패턴을 재사용할 수 있다.

## 목표

- Phase 3/5/7의 기존 API와 Evidence 재현 계약을 유지한다.
- `OrderService`와 `ProductService`에서 central switch를 제거한다.
- Order loading 전략은 strategy bean + registry 구조로 분리한다.
- Product search 전략은 strategy bean + registry 구조로 분리한다.
- Point pagination은 offset/page reader와 cursor reader로 분리하되, 응답 타입이 다르므로 단일 registry로 묶지 않는다.
- 기존 request parameter 값은 유지한다.
- 기존 k6 scenario, Grafana label, docs/evidence 경로는 유지한다.
- 기존 enum 기반 service overload는 테스트와 호환을 위해 유지할 수 있다. 단, enum은 전략 실행을 결정하는 central switch가 아니라 이름 변환 또는 compatibility layer로만 둔다.
- 이후 Phase 11 동시성 전략 구조로 확장 가능한 패턴을 만든다.

## 비목표

- 이번 변경에서 Phase 11 Concurrency Control을 구현하지 않는다.
- 이번 변경에서 재고 차감, 쿠폰 발급, idempotency API를 추가하지 않는다.
- 이번 변경에서 Phase 3/5/7 evidence를 다시 측정하지 않는다.
- 이번 변경에서 k6 script, preset JSON, Grafana dashboard YAML을 바꾸지 않는다.
- 이번 변경에서 API endpoint path를 바꾸지 않는다.
- 이번 변경에서 Offset API와 Cursor API를 하나의 endpoint로 합치지 않는다.
- 이번 변경에서 `Page<T>`와 cursor response를 하나의 공통 DTO로 합치지 않는다.
- 이번 변경에서 모든 service를 clean architecture 계층으로 재구성하지 않는다.

## 결정

이번 리팩토링은 "target별 전략군 분리"로 진행한다.

```text
Order loading strategies
Product search strategies
Point pagination readers
```

Order와 Product는 같은 입력/출력 계약 안에서 전략만 바뀌므로 registry 구조를 적용한다. Point pagination은 Offset과 Cursor의 응답 계약이 다르므로 reader class 분리까지만 적용한다.

## 목표 파일 구조

목표 구조는 다음과 같다.

```text
ecommerce/src/main/java/com/dblab/ecommerce/service/
  OrderService.java
  ProductService.java
  PointService.java

  order/
    OrderLoadingStrategy.java
    OrderLoadingStrategyRegistry.java
    LazyOrderLoadingStrategy.java
    FetchJoinOrderLoadingStrategy.java
    BatchSizeOrderLoadingStrategy.java
    EntityGraphOrderLoadingStrategy.java

  product/
    ProductSearchStrategy.java
    ProductSearchStrategyRegistry.java
    BaselineProductSearchStrategy.java
    QuerydslProductSearchStrategy.java

  point/
    OffsetPointPaginationReader.java
    CursorPointPaginationReader.java
```

현재 `com.dblab.ecommerce.service.OrderLoadingStrategy`와 `ProductSearchStrategy` enum은 compatibility를 위해 유지하거나, 위 하위 package의 enum/contract로 이동할 수 있다. 이동할 경우 controller와 tests import를 함께 갱신한다.

중요한 기준은 다음이다.

- `OrderService`는 로딩 방식별 repository 호출을 직접 알지 않는다.
- `ProductService`는 검색 방식별 repository 호출을 직접 알지 않는다.
- `PointService`는 Offset/Cursor 알고리즘을 직접 구현하지 않고 reader에 위임한다.

## Phase 3 Order Loading Strategy

### Contract

Order loading strategy는 다음 역할을 가진다.

```java
public interface OrderLoadingStrategy {
    String name();

    List<OrderResponse> loadByUserId(Long userId);
}
```

전략 이름은 기존 request parameter 값을 유지한다.

| Strategy | Name | Existing meaning |
|---|---|---|
| Lazy | `lazy` | naive N+1 baseline |
| Fetch Join | `fetch-join` | join fetch로 연관 로딩 |
| Batch Size | `batch-size` | batch-size 기반 lazy loading evidence |
| EntityGraph | `entity-graph` | JPA EntityGraph 로딩 |

`batch-size`는 현재 코드에서 `lazy`와 같은 repository method를 호출한다. 이는 의도된 Phase 3 측정 조건이다. Batch size 효과는 mapping/config 조건에서 발생하므로 별도 class로 두되 구현은 lazy reader와 같은 repository call을 사용할 수 있다.

### Registry

`OrderLoadingStrategyRegistry`는 Spring이 주입한 `List<OrderLoadingStrategy>`로 name-to-strategy map을 만든다.

요구사항:

- duplicate name이 있으면 애플리케이션 시작 시 실패한다.
- unknown name이면 명확한 `IllegalArgumentException`을 던진다.
- blank/null strategy name은 기존처럼 `lazy`로 처리한다.

예상 API:

```java
public OrderLoadingStrategy get(String name);
```

### Service

`OrderService`는 registry에 위임한다.

예상 API:

```java
public List<OrderResponse> getOrdersByUserId(Long userId);
public List<OrderResponse> getOrdersByUserId(Long userId, String strategyName);
```

기존 테스트 호환을 위해 enum overload가 필요하면 유지한다.

```java
public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategyName strategyName);
```

단, 이 overload도 switch를 갖지 않고 `strategyName.value()` 또는 `strategyName.nameValue()`를 registry에 넘긴다.

### Controller

`OrderController`의 endpoint는 유지한다.

```http
GET /api/orders?userId=100&strategy=lazy
GET /api/orders?userId=100&strategy=fetch-join
GET /api/orders?userId=100&strategy=batch-size
GET /api/orders?userId=100&strategy=entity-graph
```

controller는 strategy string을 service에 넘긴다. 기존 parameter 값과 default `lazy`는 유지한다.

## Phase 5 Product Search Strategy

### Contract

Product search strategy는 다음 역할을 가진다.

```java
public interface ProductSearchStrategy {
    String name();

    List<ProductResponse> search(Long categoryId, Product.Status status);
}
```

전략 이름은 기존 request parameter 값을 유지한다.

| Strategy | Name | Existing meaning |
|---|---|---|
| Baseline | `baseline` | Spring Data JPA entity loading 후 DTO 변환 |
| QueryDSL | `querydsl` | QueryDSL DTO projection |

### Registry

`ProductSearchStrategyRegistry`는 Spring이 주입한 `List<ProductSearchStrategy>`로 name-to-strategy map을 만든다.

요구사항:

- duplicate name이 있으면 애플리케이션 시작 시 실패한다.
- unknown name이면 명확한 `IllegalArgumentException`을 던진다.
- blank/null strategy name은 기존처럼 `querydsl`로 처리한다.

예상 API:

```java
public ProductSearchStrategy get(String name);
```

### Strategy 구현

`BaselineProductSearchStrategy`는 기존 `ProductService.findBaselineProducts()` 로직을 가져간다.

```text
categoryId + status -> productRepository.findByCategoryIdAndStatus
categoryId only     -> productRepository.findByCategoryId
status only         -> productRepository.findByStatus
no filter           -> productRepository.findAll
```

`QuerydslProductSearchStrategy`는 `ProductQueryRepository.searchProducts(categoryId, status)`에 위임한다.

### Service

`ProductService`는 registry에 위임한다.

예상 API:

```java
public List<ProductResponse> searchProducts(Long categoryId, Product.Status status);
public List<ProductResponse> searchProducts(Long categoryId, Product.Status status, String strategyName);
public List<ProductReviewSummaryResponse> getReviewSummary();
```

기존 테스트 호환을 위해 enum overload가 필요하면 유지한다.

```java
public List<ProductResponse> searchProducts(
    Long categoryId,
    Product.Status status,
    ProductSearchStrategyName strategyName
);
```

단, 이 overload도 switch를 갖지 않고 registry에 위임한다.

### Controller

`ProductController`의 endpoint는 유지한다.

```http
GET /api/products?categoryId=200&status=ON_SALE&strategy=baseline
GET /api/products?categoryId=200&status=ON_SALE&strategy=querydsl
```

기존 default `querydsl`은 유지한다. unknown strategy에 대한 `BAD_REQUEST` mapping도 유지한다.

## Phase 7 Point Pagination Readers

Phase 7은 응답 타입이 다르므로 Order/Product처럼 단일 strategy registry로 묶지 않는다.

```text
GET /api/points        -> Offset/Page
GET /api/points/cursor -> Cursor
```

### Offset Reader

`OffsetPointPaginationReader`는 기존 `PointService.getPointHistory()` 구현을 가져간다.

책임:

- `PageRequest` 생성
- 정렬 기준 유지
- `pointHistoryRepository.findByUserId(userId, pageRequest)` 호출
- `PointHistoryResponse::from` mapping

정렬 기준은 Phase 7 재측정 spec과 같이 유지한다.

```java
Sort.by(Sort.Direction.DESC, "createdAt")
    .and(Sort.by(Sort.Direction.DESC, "id"))
```

### Cursor Reader

`CursorPointPaginationReader`는 기존 `PointService.getPointHistoryByCursor()` 구현을 가져간다.

책임:

- 첫 cursor page와 next cursor page 분기
- `size + 1` 조회
- `hasNext` 계산
- `nextCursor` 생성
- `PointHistoryCursorResponse` 생성

Cursor 조건은 기존 repository 계약을 유지한다.

```text
lastCreatedAt == null || lastId == null -> first page
otherwise -> next cursor page
```

### Service

`PointService`는 두 reader에 위임한다.

예상 API는 기존과 동일하다.

```java
public Page<PointHistoryResponse> getPointHistory(Long userId, int page, int size);

public PointHistoryCursorResponse getPointHistoryByCursor(
    Long userId,
    int size,
    LocalDateTime lastCreatedAt,
    Long lastId
);
```

## Compatibility

이번 리팩토링 후에도 다음 public API는 유지되어야 한다.

```http
GET /api/orders?userId={userId}&strategy={lazy|fetch-join|batch-size|entity-graph}
GET /api/products?categoryId={categoryId}&status={status}&strategy={baseline|querydsl}
GET /api/points?userId={userId}&page={page}&size={size}
GET /api/points/cursor?userId={userId}&size={size}&lastCreatedAt={lastCreatedAt}&lastId={lastId}
```

다음 k6 scenario와 label 계약도 유지한다.

```text
orders-test.js
products-test.js
points-test.js
points-cursor-test.js
points-offset-sampling-test.js
points-cursor-sampling-test.js

phase
scenario
preset
pool
```

Phase Evidence path와 기존 docs/evidence 파일은 이동하지 않는다.

## Error Handling

Registry는 unknown strategy에 대해 명확한 `IllegalArgumentException`을 던진다.

Controller response mapping은 기존 계약을 우선한다.

- Product unknown strategy는 기존처럼 `400 BAD_REQUEST`로 매핑한다.
- Order unknown strategy는 기존 동작을 확인한 뒤, 별도 API 계약 변경 없이 처리한다. 이번 spec의 핵심은 내부 전략 실행 구조이며, unknown strategy response contract 변경은 필수 범위가 아니다.
- Point pagination은 endpoint가 분리되어 있으므로 strategy name unknown case가 없다.

필요하면 후속 작업에서 공통 controller advice로 unsupported strategy error를 일관되게 정리한다.

## 테스트 전략

### Order

확인할 내용:

- blank/null strategy는 `lazy`로 처리된다.
- `lazy`, `fetch-join`, `batch-size`, `entity-graph` 이름이 registry에서 resolve된다.
- unknown name은 명확한 예외를 던진다.
- `OrderService.getOrdersByUserId(userId)`는 기존 lazy behavior를 유지한다.
- `OrderService.getOrdersByUserId(userId, "fetch-join")`는 fetch join strategy를 사용한다.
- 기존 Phase 3 repository tests는 계속 통과한다.

### Product

확인할 내용:

- blank/null strategy는 `querydsl`로 처리된다.
- `baseline`, `querydsl` 이름이 registry에서 resolve된다.
- unknown name은 명확한 예외를 던진다.
- baseline과 querydsl이 기존 fixture에서 같은 응답을 반환한다.
- 두 argument overload는 기존처럼 querydsl을 기본으로 사용한다.
- 기존 `ProductSearchStrategyTest`의 behavior가 유지된다.

### Point

확인할 내용:

- Offset/Page API는 기존 `Page<PointHistoryResponse>` 형태를 유지한다.
- Offset/Page 정렬은 `createdAt DESC, id DESC`를 유지한다.
- Cursor 첫 페이지는 `items`, `nextCursor`, `hasNext`를 반환한다.
- Cursor 다음 페이지는 이전 cursor 이후 slice를 반환한다.
- 마지막 페이지에서 `hasNext=false`, `nextCursor=null`을 유지한다.
- 기존 `PointCursorApiTest`가 계속 통과한다.

### 전체 검증

주요 검증 명령:

```bash
cd ecommerce && rtk gradlew test
```

필요하면 focused test를 먼저 실행한다.

```bash
cd ecommerce && rtk gradlew test --tests "*OrderLoadingStrategyTest"
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyTest"
cd ecommerce && rtk gradlew test --tests "*PointCursorApiTest"
```

## 마이그레이션 순서

1. 현재 `OrderService`, `ProductService`, `PointService` behavior를 focused test로 확인한다.
2. Order loading contract와 registry를 만든다.
3. Lazy, Fetch Join, Batch Size, EntityGraph order loading strategy class를 만든다.
4. `OrderService`를 registry delegation 구조로 바꾼다.
5. `OrderController`가 strategy string을 service에 넘기도록 정리한다.
6. Product search contract와 registry를 만든다.
7. Baseline, QueryDSL product search strategy class를 만든다.
8. `ProductService`를 registry delegation 구조로 바꾼다.
9. Product controller/test compatibility를 확인한다.
10. Offset/Cursor point pagination reader를 만든다.
11. `PointService`를 reader delegation 구조로 바꾼다.
12. focused tests와 전체 test를 실행한다.

## 완료 기준

- `OrderService`에 order loading strategy switch가 없다.
- `ProductService`에 product search strategy switch가 없다.
- `PointService`는 offset/cursor pagination 알고리즘을 직접 구현하지 않고 reader에 위임한다.
- 기존 API endpoint path와 request parameter 값이 유지된다.
- 기존 k6 scenario와 Grafana label 계약이 유지된다.
- Phase 3/5/7 focused tests가 통과한다.
- 전체 Gradle test가 통과한다.
- 새 조회 전략을 추가할 때 기존 strategy class를 수정하지 않고 새 class와 registry entry로 확장할 수 있다.

## 이후 Phase 연결

이 리팩토링은 Phase 11 Concurrency Control의 직접 구현이 아니다. 하지만 다음 구조를 준비한다.

```text
StockDeductionStrategy
  - pessimistic
  - optimistic
  - atomic-update
  - serializable-retry

CouponIssueStrategy
  - pessimistic
  - atomic-update
  - serializable-retry

IdempotentOrderUseCase
  - idempotency key 저장소
  - 기존 order/concurrency strategy 위임
```

Phase 11에서 이 패턴을 적용할 때도 하나의 거대한 service switch를 만들지 않는다. 전략 이름은 Measurement Condition과 k6/Grafana label에 쓰이는 low-cardinality 값과 맞춰 관리한다.
