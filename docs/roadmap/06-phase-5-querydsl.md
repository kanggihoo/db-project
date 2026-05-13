# 이커머스 DB 최적화 학습 로드맵

## Phase 5. 쿼리 최적화 + QueryDSL

> "필요한 것만 정확하게 가져오고, 복잡한 동적 쿼리를 타입 안전하게 작성한다."

### 이전 Phase의 문제를 어떻게 해결하는가

- Entity 전체 조회 → DTO Projection으로 불필요한 컬럼 제거
- 문자열 JPQL의 동적 쿼리 한계 → QueryDSL로 타입 안전한 동적 쿼리
- 단건 루프 업데이트 → 벌크 연산으로 DB 왕복 횟수 감소

### 실험 1: DTO Projection

```java
// Before: User 엔티티 전체 조회 (불필요한 password, point_balance 등 포함)
List<User> users = userRepo.findAll();

// After: 필요한 컬럼만
@Query("SELECT new com.example.dto.UserSummary(u.id, u.name, u.grade) FROM User u")
List<UserSummary> findAllSummary();

// QueryDSL로
List<UserSummary> result = queryFactory
    .select(Projections.constructor(UserSummary.class,
        user.id, user.name, user.grade))
    .from(user)
    .fetch();
```

### 실험 2: QueryDSL 동적 상품 검색

```java
// 검색 조건: 카테고리, 가격 범위, 상태, 키워드 — 모두 선택적
public List<ProductDto> search(ProductSearchCondition condition) {
    return queryFactory
        .select(Projections.constructor(ProductDto.class, ...))
        .from(product)
        .where(
            categoryEq(condition.getCategoryId()),     // null이면 조건 제외
            priceBetween(condition.getMinPrice(), condition.getMaxPrice()),
            statusEq(condition.getStatus()),
            nameContains(condition.getKeyword())
        )
        .fetch();
}
```

### 실험 3: 벌크 연산

```java
// Before: 단건 루프 (주문 상태 일괄 변경)
// UPDATE 쿼리가 N번 나감
List<Order> orders = orderRepo.findByStatus(PAID);
orders.forEach(o -> o.updateStatus(PREPARING));  // dirty checking → N번 UPDATE

// After: 벌크 연산 (1번)
@Modifying
@Query("UPDATE Order o SET o.status = :newStatus WHERE o.status = :oldStatus")
int bulkUpdateStatus(@Param("oldStatus") OrderStatus old,
                     @Param("newStatus") OrderStatus newStatus);
```

> **주의:** 벌크 연산 후 영속성 컨텍스트 초기화 필요 (`@Modifying(clearAutomatically = true)`)

### 모니터링으로 확인하는 것

- DTO vs Entity 조회 응답 데이터 크기 (bytes) 비교
- 단건 루프 vs 벌크 연산 실행시간 (1000건 기준)

### 이 Phase에서 얻는 인사이트

- Entity 조회가 항상 옳은 게 아닌 이유 — 화면에 필요한 데이터만
- QueryDSL의 타입 안전성이 실무에서 왜 중요한가
- 벌크 연산과 영속성 컨텍스트의 충돌 — `clearAutomatically`의 의미

### 측정 지표 (회고용)

- Entity vs DTO 응답 데이터 크기 차이 (%)
- 단건 루프 vs 벌크 연산 시간 차이 (1000건 기준 ms)

### 남은 문제 → Phase 6으로

> "단순 조회는 최적화했는데, GROUP BY 집계 쿼리가 느리다. 인덱스가 집계에도 영향을 미치는가?"

---
