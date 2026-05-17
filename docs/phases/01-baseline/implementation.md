# Phase 1 구현 계획: 나이브한 구현 + 베이스라인 확보

## Context

PHASE1.md에 정의된 대로, 최적화 없는 나이브한 구현을 통해 N+1, 풀스캔, 느린 페이지네이션 문제의 베이스라인 수치를 확보한다.
이후 Phase 2~7에서의 "Before" 기준점으로 활용된다.

**현재 상태:**
- 24개 엔티티 클래스 완성 (최적화 없는 원시 상태, JPA 연관관계 매핑 미적용)
- BulkInsertRepository + DataSeeder 완성
- Testcontainers 기반 테스트 인프라 완성 (init.sql + TestcontainersConfiguration)
- 아직 없는 것: Repository(JPA), Service, Controller, k6 스크립트

**TDD 진행 방식:** 테스트 코드 작성 → 컴파일 확인 → Red 확인 → 구현 코드 작성 → Green 확인

---

## 핵심 제약: 나이브함(Naiveness) 정의

Phase 1에서 의도적으로 **하지 않는 것:**
- `@OneToMany`, `@ManyToOne` JPA 연관관계 매핑 — 엔티티는 현재처럼 `Long` FK 필드만 보유
- `@BatchSize`, `@EntityGraph`, `JOIN FETCH` 금지
- DB 인덱스 추가 금지
- DTO Projection 금지 — 항상 Entity 전체 로드

N+1은 **서비스 레이어에서 루프로 직접 추가 쿼리를 발생**시켜 구현한다.

---

## 구현 대상 파일 목록

### 1단계: JPA Repository (Shell → Test → Implement)

| 파일 | 경로 |
|------|------|
| `OrderRepository` | `src/main/java/com/dblab/ecommerce/repository/OrderRepository.java` |
| `OrderItemRepository` | `src/main/java/com/dblab/ecommerce/repository/OrderItemRepository.java` |
| `ProductRepository` | `src/main/java/com/dblab/ecommerce/repository/ProductRepository.java` |
| `PointHistoryRepository` | `src/main/java/com/dblab/ecommerce/repository/PointHistoryRepository.java` |

### 2단계: Service Layer

| 파일 | 경로 |
|------|------|
| `OrderService` | `src/main/java/com/dblab/ecommerce/service/OrderService.java` |
| `ProductService` | `src/main/java/com/dblab/ecommerce/service/ProductService.java` |
| `PointService` | `src/main/java/com/dblab/ecommerce/service/PointService.java` |

### 3단계: Controller Layer

| 파일 | 경로 |
|------|------|
| `OrderController` | `src/main/java/com/dblab/ecommerce/controller/OrderController.java` |
| `ProductController` | `src/main/java/com/dblab/ecommerce/controller/ProductController.java` |
| `PointController` | `src/main/java/com/dblab/ecommerce/controller/PointController.java` |

### 4단계: Response DTO (단순 래퍼)

| 파일 | 경로 |
|------|------|
| `OrderResponse` | `src/main/java/com/dblab/ecommerce/dto/OrderResponse.java` |
| `ProductResponse` | `src/main/java/com/dblab/ecommerce/dto/ProductResponse.java` |
| `PointHistoryResponse` | `src/main/java/com/dblab/ecommerce/dto/PointHistoryResponse.java` |

### 5단계: 테스트 파일

| 파일 | 경로 | 검증 목적 |
|------|------|-----------|
| `OrderRepositoryTest` | `src/test/java/.../repository/OrderRepositoryTest.java` | N+1: `getPrepareStatementCount()` 전후 차이 = orders.size() |
| `ProductRepositoryTest` | `src/test/java/.../repository/ProductRepositoryTest.java` | Seq Scan 유발: categoryId + status 필터 결과 건수 검증 |
| `PointHistoryRepositoryTest` | `src/test/java/.../repository/PointHistoryRepositoryTest.java` | Offset 페이징 반환 건수 및 페이지 범위 검증 |

### 6단계: k6 스크립트

| 파일 | 경로 |
|------|------|
| `orders-test.js` | `k6/orders-test.js` |
| `products-test.js` | `k6/products-test.js` |
| `points-test.js` | `k6/points-test.js` |

---

## 상세 구현 계획

### Step 1: application.yaml 설정 추가

`src/main/resources/application.yaml`에 추가:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
  hikari:
    maximum-pool-size: 10
```

`src/main/resources/application.yaml`에 web starter 의존성이 없으므로 `build.gradle`에 추가:
```groovy
implementation 'org.springframework.boot:spring-boot-starter-web'
```

### Step 2: TDD 사이클 — OrderRepository (N+1 기폭제)

**TDD Red:**
```java
// OrderRepositoryTest.java
// findByUserId 메서드가 없어서 컴파일 에러 → Shell 코드 필요
List<Order> orders = orderRepository.findByUserId(1L); // RED
```

**Shell (컴파일 통과용):**
```java
// OrderRepository.java
public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserId(Long userId);
}
```

**OrderItemRepository Shell:**
```java
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
}
```

**테스트 구현 (PHASE1.md 스펙 그대로):**
- `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = Replace.NONE)`
- `@Import(TestcontainersConfiguration.class)`
- Hibernate Statistics로 `getPrepareStatementCount()` 전후 측정
- `extraSqlCount == orders.size()` 단언

### Step 3: TDD 사이클 — ProductRepository (풀스캔 유도)

**Shell:**
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndStatus(Long categoryId, Product.Status status);
}
```

**테스트:**
- `findByCategoryIdAndStatus()` 반환 결과가 비어있지 않음 단언
- 인덱스 없는 상태에서 결과 건수 검증 (Seq Scan은 EXPLAIN ANALYZE로 확인 — 테스트에선 결과 건수만 단언)

### Step 4: TDD 사이클 — PointHistoryRepository (Offset 페이징)

**Shell:**
```java
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Page<PointHistory> findByUserId(Long userId, Pageable pageable);
}
```

**테스트:**
- `PageRequest.of(0, 10)` → 첫 페이지 건수 = 10 (또는 전체 데이터 수 중 작은 값)
- `PageRequest.of(100, 10)` → 반환 결과가 빈 페이지이거나 정상 반환됨

### Step 5: Service 레이어 구현

**OrderService — N+1 의도적 유발:**
```java
public List<OrderResponse> getOrdersByUserId(Long userId) {
    List<Orders> orders = orderRepository.findByUserId(userId);
    return orders.stream().map(order -> {
        // LAZY 로드 대신 직접 추가 쿼리 (N+1 기폭제)
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return OrderResponse.of(order, items);
    }).toList();
}
```

**ProductService — 필터 조회:**
```java
public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
    return productRepository.findByCategoryIdAndStatus(categoryId, status)
        .stream().map(ProductResponse::from).toList();
}
```

**PointService — Offset 페이징:**
```java
public Page<PointHistoryResponse> getPointHistory(Long userId, int page, int size) {
    return pointHistoryRepository.findByUserId(userId, PageRequest.of(page, size))
        .map(PointHistoryResponse::from);
}
```

### Step 6: Controller 레이어 구현

```
GET /api/orders?userId={userId}          → OrderController
GET /api/products?categoryId=&status=   → ProductController  
GET /api/points?userId=&page=&size=     → PointController
```

### Step 7: k6 스크립트 작성

공통 스펙: VU 50, Duration 5분, Ramp-up 30초, timeout 5s

**orders-test.js**: `userId` 1~1000 랜덤
**products-test.js**: `categoryId` 1~20 랜덤, `status` 3가지 중 랜덤
**points-test.js**: `page` 가중 분포 — 50% → 1~10, 30% → 50~100, 20% → 500~1000

---

## TDD 실행 순서 (gradlew 명령)

```bash
# 1. 컴파일 확인 (Shell 코드 작성 후)
./gradlew compileTestJava

# 2. Red 확인 (테스트 코드 작성 후, 구현 전)
./gradlew test --tests "*.OrderRepositoryTest"

# 3. 구현 후 Green 확인
./gradlew test --tests "*.OrderRepositoryTest"

# 4. 전체 테스트
./gradlew test
```

---

## 기존 재사용 파일

| 재사용 파일 | 경로 | 재사용 방법 |
|-------------|------|------------|
| `TestcontainersConfiguration` | `src/test/java/com/dblab/ecommerce/TestcontainersConfiguration.java` | `@Import`로 모든 Repository 테스트에서 재사용 |
| `init.sql` | `src/test/resources/init.sql` | Testcontainer 스키마 초기화 — 그대로 사용 |
| `application-test.yaml` | `src/test/resources/application-test.yaml` | `show-sql: true` 이미 설정 |
| 24개 Entity | `src/main/java/.../entity/` | FK 필드(`Long`) 그대로 사용, JPA 연관관계 추가 없음 |

---

## 완료 조건

- [ ] `./gradlew compileTestJava` 통과
- [ ] `OrderRepositoryTest`: `extraSqlCount == orders.size()` GREEN
- [ ] `ProductRepositoryTest`: `findByCategoryIdAndStatus()` 결과 건수 검증 GREEN
- [ ] `PointHistoryRepositoryTest`: 페이지 반환 검증 GREEN
- [ ] `./gradlew test` 전체 GREEN
- [ ] 3개 API 엔드포인트 정상 응답 확인
- [ ] k6 스크립트 3개 작성 완료
