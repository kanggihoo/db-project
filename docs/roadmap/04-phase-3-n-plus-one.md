# 이커머스 DB 최적화 학습 로드맵

## Phase 3. N+1 + 로딩 전략 최적화

> "쿼리가 101번 나가는 이유와, 1번으로 줄이는 방법들의 트레이드오프"

### 이전 Phase의 문제를 어떻게 해결하는가

인덱스로도 해결 안 되는 쿼리 수 자체의 문제 — N+1을 발생시키고, 각 해결책을 순서대로 적용하며 비교한다.

### N+1 대표 시나리오

```
주문 목록 100건 조회
  → ORDER 1번 쿼리
  → ORDER_ITEM N번 쿼리  (주문마다 1번)
  → SKU N번 쿼리
  → PRODUCT N번 쿼리
  → PRODUCT_IMAGE N번 쿼리
= 총 401번 쿼리 발생
```

### 단계별 해결 비교

| #   | 방법                    | 특징                               | 적합한 상황                  |
| --- | ----------------------- | ---------------------------------- | ---------------------------- |
| 1   | **Lazy Loading (기본)** | 접근할 때마다 쿼리 — N+1 발생      | 연관 데이터를 거의 안 쓸 때  |
| 2   | **Eager Loading**       | 항상 JOIN — 불필요한 데이터도 로딩 | 항상 연관 데이터가 필요할 때 |
| 3   | **Fetch Join**          | JPQL JOIN FETCH — 쿼리 1번         | 단일 컬렉션, 페이징 없을 때  |
| 4   | **EntityGraph**         | 어노테이션 기반 Fetch Join         | 동적으로 로딩 전략 변경할 때 |
| 5   | **BatchSize**           | IN 쿼리로 묶음 조회                | 다중 컬렉션, 페이징 있을 때  |

### 핵심 함정: Fetch Join + 페이징

```java
// 이 코드는 HibernateJpaDialect 경고 발생
// "HHH90003004: firstResult/maxResults specified with collection fetch"
// 전체 데이터를 메모리로 올린 뒤 페이징 → OOM 위험
@Query("SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.user.id = :userId")
Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);

// 해결: BatchSize로 페이징 + N+1 동시 해결
@BatchSize(size = 100)
@OneToMany(mappedBy = "order")
private List<OrderItem> orderItems;
```

### 부가 실험

```java
// 1차 캐시 확인: 같은 트랜잭션 안에서 중복 쿼리 발생 여부
@Transactional
public void test() {
    Product p1 = productRepo.findById(1L); // 쿼리 발생
    Product p2 = productRepo.findById(1L); // 쿼리 발생 안 함 (1차 캐시)
}

// readOnly=true: dirty checking 생략으로 조회 성능 개선
@Transactional(readOnly = true)
public List<OrderResponse> getOrders(Long userId) { ... }
```

### 모니터링으로 확인하는 것

- 쿼리 실행 수 Before/After (Hibernate 통계 또는 `p6spy`)
- k6: 주문 목록 API 응답시간 각 방법별 비교
- Grafana: 방법별 쿼리 수 + 응답시간 한 화면에 비교

### 이 Phase에서 얻는 인사이트

- N+1은 Lazy Loading의 부작용이 아니라 **연관 데이터 접근 패턴의 문제**
- Fetch Join은 만능이 아니다 — 페이징과 함께 쓰면 OOM 위험
- BatchSize가 실무에서 가장 안전한 선택인 이유

### 측정 지표 (회고용)

- 방법별 발생 쿼리 수 (Lazy: 401회 → BatchSize: 5회)
- 방법별 응답시간 (ms)
- readOnly=true 적용 전/후 메모리 사용량 차이

### 남은 문제 → Phase 4로

> "쿼리 수는 줄었는데, 동시에 여러 사용자가 요청하면 데이터 정합성은 어떻게 되는가?"

### 완료 조건

- [ ] 주문 목록 조회에서 N+1이 발생하는 조건과 쿼리 수를 재현했다.
- [ ] Fetch Join, EntityGraph, BatchSize 중 최소 2개 전략을 적용해 비교했다.
- [ ] Fetch Join과 페이징 조합의 위험 또는 제한을 확인했다.
- [ ] 최적화 전/후 쿼리 수와 응답시간을 기록했다.
- [ ] 동시 요청 시 데이터 일관성 관점의 다음 실험 질문을 Phase 4로 연결했다.

---
