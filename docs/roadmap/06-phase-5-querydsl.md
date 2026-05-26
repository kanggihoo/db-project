# 이커머스 DB 최적화 학습 로드맵
## Phase 5. 쿼리 최적화 + QueryDSL

> "화면에 필요한 데이터만 정확히 조회하고, 복잡한 동적 쿼리를 타입 안전하게 작성한다."

### 현재 코드와 Phase 연결

- Phase 3에서는 `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` 조회 경로의 N+1 문제와 로딩 전략을 비교했다. QueryDSL DTO projection은 Phase 5 범위로 남겨두었다.
- Phase 4에서는 트랜잭션 격리와 동시 갱신 충돌을 정리했다. Phase 5는 데이터 정합성 단계가 아니라 조회 계층의 DTO projection과 동적 조건 조합에 집중한다.
- `ecommerce/build.gradle`에는 QueryDSL 5.1.0 Jakarta 의존성이 이미 있다. Phase 5는 Q-class 생성 확인, 상품 검색 구현, 증거 기록에 집중한다.
- 기존 상품 검색 API는 유지한다. Phase 5는 학습 증거 비교를 위해 `GET /api/products`에 `strategy=baseline|querydsl`을 노출한다.
- `baseline`은 Spring Data JPA 엔티티 조회를 유지하고 각 엔티티를 `ProductResponse.from(product)`으로 변환한다.
- `querydsl`은 `ProductResponse` 필드로 직접 DTO projection을 수행하며, `strategy`가 생략되면 기본값으로 사용된다.

### 최적화 대상

- 엔티티 조회 후 DTO 변환 -> DTO projection으로 불필요한 컬럼 조회와 엔티티 materialization을 줄인다.
- 문자열 기반 쿼리 또는 derived query의 동적 조건 한계 -> QueryDSL로 타입 안전한 조건 조합을 사용한다.
- 행 단위 상태 변경 -> JPQL bulk update로 prepared statement 수를 줄인다.

재고/쿠폰 동시성에 필요한 atomic update, pessimistic lock, optimistic lock, retry, idempotency 전략은 Phase 11 범위로 남긴다. Phase 5의 bulk update 증거는 대량 상태 변경과 persistence context 동작 확인으로 제한한다.

### 실험 1: DTO Projection

```java
// Before: 응답에 필요하지 않은 컬럼까지 포함해 엔티티 전체를 조회
List<Product> products = productRepository.findByCategoryIdAndStatus(categoryId, status);
List<ProductResponse> responses = products.stream()
    .map(ProductResponse::from)
    .toList();

// After: QueryDSL projection으로 응답 필드만 선택
QProduct product = QProduct.product;

List<ProductResponse> responses = queryFactory
    .select(Projections.constructor(ProductResponse.class,
        product.id,
        product.categoryId,
        product.name,
        product.basePrice,
        product.status))
    .from(product)
    .where(
        categoryEq(categoryId),
        statusEq(status)
    )
    .fetch();
```

### 실험 2: 상품 검색 전략

구현된 비교는 기존 `GET /api/products` API를 사용한다.

- `GET /api/products?strategy=baseline&categoryId=200&status=ON_SALE`
- `GET /api/products?strategy=querydsl&categoryId=200&status=ON_SALE`
- `GET /api/products?categoryId=200&status=ON_SALE`은 `querydsl`을 기본값으로 사용한다.

두 전략은 동일한 `categoryId`와 `status` 조건에서 같은 `ProductResponse` 값을 반환한다. QueryDSL 경로는 null 조건을 predicate에서 안전하게 제외하는지도 검증한다.

### 실험 3: Bulk Update

```java
// Before: 행 단위 baseline
List<Orders> orders = orderRepository.findByStatus(Orders.Status.PENDING);
orders.forEach(Orders::markPreparing);
entityManager.flush();

// After: bulk update
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE Orders o SET o.status = :newStatus WHERE o.status = :oldStatus")
int bulkUpdateStatus(@Param("oldStatus") Orders.Status oldStatus,
                     @Param("newStatus") Orders.Status newStatus);
```

Bulk update는 managed entity 상태를 우회한다. 따라서 repository 메서드는 `clearAutomatically = true`와 `flushAutomatically = true`를 사용한다. 증거는 bulk update 이후 먼저 로딩된 주문을 데이터베이스에서 다시 읽어 변경 상태를 확인하는 동작을 기록한다.

### 필수 증거

Phase 5 필수 증거는 `docs/evidence/phase-05/` 아래에 저장한다.

- focused integration test 출력
- baseline 및 QueryDSL 대표 SQL
- 상품 검색과 bulk update의 SQL count 증거
- 상품 검색과 bulk update 요약 문서

k6/Grafana와 `pg_stat_statements`는 이 phase의 선택 증거이며 closeout 필수 조건은 아니다.

### 측정 결과

- 상품 검색 baseline은 Product 엔티티 컬럼을 조회한 뒤 `ProductResponse.from(product)`으로 변환했다.
- 상품 검색 QueryDSL 경로는 `ProductResponse`에 필요한 필드만 선택했다.
- 공유 fixture 조건에서 두 상품 검색 전략은 각각 SQL statement 1개를 사용했다.
- 행 단위 dirty checking은 fixture 기준 Hibernate `prepareStatementCount=4`를 기록했다. 이는 select 1회와 update 3회이며 `entityUpdateCount=3`이다.
- JPQL bulk update는 같은 3개 row를 Hibernate `prepareStatementCount=1`로 변경했다.

### 인사이트

- 응답이 일부 컬럼만 필요로 할 때 엔티티 로딩이 항상 적절한 read model은 아니다.
- QueryDSL은 타입 안전한 DTO projection과 optional predicate 조합에 유용하다.
- Bulk update는 SQL 수를 줄일 수 있지만 persistence context 처리를 명시해야 한다.
- 조회 최적화와 동시성 제어는 별도 문제다. 재고/쿠폰 동시성은 Phase 11 범위로 유지한다.

### 남은 질문 -> Phase 6

> "단순 조회는 최적화했지만 GROUP BY 집계 쿼리는 여전히 느리다. 인덱스가 Product/Review 집계 쿼리 성능에도 영향을 주는가?"

Phase 6은 Product/Review 집계 쿼리, `GROUP BY`, `HAVING`, expression index, 실행 계획 비교로 이어간다.

### 완료 조건

- [x] 기존 엔티티 조회 후 `ProductResponse.from(product)` baseline을 유지하고 QueryDSL DTO projection과 같은 측정 조건에서 비교했다.
- [x] `GET /api/products`가 `strategy=baseline|querydsl`을 지원한다.
- [x] `strategy` 생략 시 `querydsl`을 기본값으로 사용한다.
- [x] baseline과 QueryDSL이 공유 `categoryId`, `status` 조건에서 같은 `ProductResponse` 값을 반환한다.
- [x] QueryDSL이 null predicate를 focused test에서 안전하게 생략한다.
- [x] 행 단위 update와 bulk update의 SQL count를 기록했다.
- [x] Bulk update persistence context 동작을 문서화하고 검증했다.
- [x] Phase evidence를 `docs/evidence/phase-05/` 아래에 정리했다.
- [x] Product/Review 집계 쿼리 후속 작업을 Phase 6으로 넘겼다.

---
