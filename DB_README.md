# 이커머스 DB 최적화 학습 로드맵

> **핵심 철학:** "최적화 없는 나이브한 구현"에서 시작하여, 각 Phase에서 **발생하는 문제를 수치로 직접 확인**하고, 그 해결책을 **하나씩** 도입한다.
> 단순히 "개선했다"가 아닌, **k6 + Grafana로 Before/After를 정량적으로 증명**하는 것이 목표다.

---

## 기술 스택 (Tech Stack)

### Backend

- **Framework**: Spring Boot 4.x
- **Build Tool**: Gradle 9.x
- **Language**: Java 21 (LTS)
- **Libraries**: Spring Data JPA, QueryDSL, Micrometer Prometheus
- **Testing**: JUnit 6, Testcontainers, Spring Boot Test, AssertJ

### Infrastructure (Docker Compose)

- **RDBMS**: PostgreSQL 17-alpine
- **Monitoring**: Prometheus (latest), Grafana (latest)
- **Load Testing**: k6 (latest, Docker 이미지 사용)
- **Dummy Data**: Java Faker (100만 건 이상 대용량 데이터 생성)

### Monitoring & Observability

> **모든 Phase에서 Prometheus + Grafana를 상시 가동.**
> 각 Phase의 문제가 Grafana 대시보드에서 수치로 보여야 "인사이트"가 완성된다.

- **Spring Metrics**: Micrometer → Prometheus → Grafana
- **DB Metrics**: postgres-exporter → Prometheus
- **k6 결과**: `--out experimental-prometheus-rw` → Prometheus → Grafana
- **쿼리 분석**: PostgreSQL `pg_stat_statements`, `EXPLAIN ANALYZE`

#### Phase별 핵심 Grafana 패널

| Phase | 핵심 지표                                        | 무엇을 보는가                                                |
| ----- | ------------------------------------------------ | ------------------------------------------------------------ |
| 0     | DB 커넥션 수, 데이터 건수                        | 인프라 정상 가동 + 더미 데이터 삽입 확인                     |
| 1     | k6 p95 응답시간, DB 커넥션 수                    | 최적화 없는 베이스라인 수치 확보                             |
| 2     | 쿼리 실행시간 (인덱스 전/후)                     | 풀스캔 → 인덱스 스캔 ms 차이                                |
| 3     | 발생 쿼리 수 (N+1 전/후)                         | 주문 100건 조회 시 쿼리 101번 → 1번                          |
| 4     | 격리 수준별 에러율, 동시 처리 RPS                | SERIALIZABLE 직렬화 실패 빈도, 격리 수준별 처리량 변화       |
| 5     | 응답시간, 데이터 전송량                          | Entity vs DTO, 단건 루프 vs 벌크 차이                        |
| 6     | 집계 쿼리 실행시간 (인덱스 전/후)                | GROUP BY에 인덱스가 미치는 영향                              |
| 7     | p95 응답시간 (페이지 번호별)                     | 1페이지 vs 1000페이지 응답시간                               |

### Testing & Verification

- **Integration Test**: JUnit 5 + Testcontainers
- **쿼리 검증**: `@DataJpaTest` + 실행 쿼리 카운트 검증
- **부하 테스트**: k6 시나리오별 VU, 지속 부하

---

## ERD

### 회원 도메인

```
USER
  id, email, password, name, phone, gender(MALE/FEMALE/OTHER), birth_date,
  grade(BRONZE/SILVER/GOLD/VIP), point_balance, created_at, updated_at

USER_ADDRESS
  id, user_id, address, detail_address,
  is_default, receiver_name, receiver_phone
```

### 상품 도메인

```
CATEGORY
  id, parent_id(self-join), name, depth
  └─ parent_id 셀프 조인으로 대/중/소 계층 표현

PRODUCT
  id, category_id, name, description,
  base_price, status(ON_SALE/SOLD_OUT/DISCONTINUED),
  is_deleted(boolean, default false), created_at, updated_at
  └─ is_deleted: Soft Delete용. Phase 2에서 부분 인덱스 실험 대상

PRODUCT_OPTION
  id, product_id, option_name   -- 색상, 사이즈 등 옵션 종류

PRODUCT_OPTION_VALUE
  id, option_id, value          -- 빨강, 파랑, M, L 등 실제 값

PRODUCT_SKU
  id, product_id, sku_code, stock_quantity, extra_price
  └─ 옵션 조합마다 재고와 추가 금액 별도 관리
  └─ CHECK (stock_quantity >= 0) — 음수 재고 방지, Phase 4 격리 수준 실험 전제조건

PRODUCT_SKU_OPTION
  id, sku_id, option_value_id   -- SKU ↔ 옵션값 연결 중간 테이블

PRODUCT_IMAGE
  id, product_id, image_url, is_main, sort_order
```

### 주문 도메인

```
CART
  id, user_id, created_at

CART_ITEM
  id, cart_id, sku_id, quantity, added_at

ORDER
  id, user_id, address_id, used_coupon_id(nullable, USER_COUPON 참조),
  total_price, discount_price, final_price,
  status(PENDING/PAID/PREPARING/SHIPPED/DELIVERED/CANCELLED), created_at
  └─ used_coupon_id: 주문 1건당 쿠폰 1장만 적용 가능

ORDER_ITEM
  id, order_id, sku_id, product_name, option_info,
  quantity, unit_price, status
  └─ 주문 시점 상품명/옵션을 스냅샷 저장 (이후 상품 변경과 무관)

DELIVERY
  id, order_id, status(PREPARING/SHIPPED/DELIVERING/DELIVERED),
  tracking_number, created_at

DELIVERY_TRACKING
  id, delivery_id, status, location, created_at
  └─ 배송 이력이 계속 쌓이는 구조 → 페이지네이션 실험에 최적
```

### 결제 도메인

```
PAYMENT
  id, order_id, method(CARD/KAKAO_PAY/NAVER_PAY),
  amount, status(PENDING/COMPLETED/FAILED/REFUNDED),
  pg_transaction_id, created_at, paid_at
  └─ paid_at: 결제 완료 시각 (created_at은 결제 시도 시각)

REFUND
  id, payment_id, order_item_id, amount, reason, status, created_at
```

### 혜택 도메인

```
COUPON
  id, name, discount_type(RATE/FIXED),
  discount_value, min_order_amount,
  started_at, expired_at,
  max_issue_count, issued_count
  └─ started_at ~ expired_at: 쿠폰 유효 기간
  └─ max_issue_count / issued_count: 선착순 발급 제한

USER_COUPON
  id, user_id, coupon_id, is_used, used_at

POINT_HISTORY
  id, user_id, type(EARN/USE/EXPIRE),
  amount, balance_after, description, created_at
  └─ 계속 쌓이는 이력 테이블 → 페이지네이션 실험에 최적
```

### 리뷰 도메인

```
REVIEW
  id, user_id, product_id, order_item_id,
  rating, content, created_at

REVIEW_IMAGE
  id, review_id, image_url

REVIEW_LIKE
  id, review_id, user_id, created_at
```

### ERD 최적화 포인트 요약

| 포인트            | 테이블                                                              | Phase   |
| ----------------- | ------------------------------------------------------------------- | ------- |
| N+1 대표 케이스   | ORDER → ORDER_ITEM → SKU → PRODUCT → PRODUCT_IMAGE                  | Phase 3 |
| 복합 인덱스 실험  | ORDER(user_id + created_at), PRODUCT(category_id + status)          | Phase 2 |
| 격리 수준 실험    | ORDER(동시 주문 시 Phantom Read), PRODUCT_SKU(재고 읽기 일관성)     | Phase 4 |
| 집계 쿼리 실험    | REVIEW(상품별 평점), ORDER(등급별 구매통계), PRODUCT(카테고리별 수) | Phase 6 |
| Soft Delete 함정  | PRODUCT(is_deleted + 부분 인덱스)                                   | Phase 2 |
| 페이지네이션 실험 | DELIVERY_TRACKING, POINT_HISTORY (계속 쌓이는 이력)                 | Phase 7 |

---

## 문제 → 해결 흐름

```
Phase 0: 프로젝트 초기 셋팅
  └─ 인프라(Docker Compose), 스키마 생성, 더미 데이터 삽입
  └─ 상세: SETUP.md 참고
       │
Phase 1: 나이브한 구현 + 베이스라인 확보
  └─ 발견: 풀스캔, N+1, 느린 페이지네이션 — 베이스라인 수치 확보
       │
Phase 2: 인덱스 설계 + 실행계획 분석
  └─ 해결: 풀스캔 → 인덱스 스캔
  └─ 발견: 복합 인덱스 순서 함정, Soft Delete가 인덱스를 무력화
       │
Phase 3: N+1 + 로딩 전략 최적화
  └─ 해결: 쿼리 수 101개 → 1개
  └─ 발견: 무분별한 Fetch Join의 페이징 경고, 로딩 전략 선택 기준
       │
Phase 4: 트랜잭션 격리 수준
  └─ 발견: 쿼리 수는 줄었는데, 동시 요청 시 데이터 정합성은 어떻게 되는가?
  └─ 해결: 격리 수준에 따라 동일 쿼리의 읽기 결과가 달라지는 현상을 실험
  └─ 발견: SERIALIZABLE은 정합성은 완벽하지만 재시도 로직 필수
       │
Phase 5: 쿼리 최적화 + QueryDSL
  └─ 해결: DTO Projection, 동적 쿼리, 벌크 연산
  └─ 발견: 필요 없는 컬럼까지 가져오는 문제, 동적 조건의 JPQL 한계
       │
Phase 6: 집계 쿼리 최적화
  └─ 해결: GROUP BY + 인덱스 최적화
  └─ 발견: 복잡한 집계 쿼리에서 인덱스가 GROUP BY에 미치는 영향
       │
Phase 7: 페이지네이션 최적화
  └─ 해결: Offset → Cursor 전환, Count 쿼리 분리
  └─ 발견: Cursor 방식의 트레이드오프 (정렬 제약, 구현 복잡도)
```

---

## Phase 0. 프로젝트 초기 셋팅

> 모든 Phase의 전제조건. 최초 1회 실행하면 Docker 볼륨으로 유지된다.

- 인프라 구성, 의존성 설정, 스키마 생성, 더미 데이터 삽입
- **상세 가이드: [SETUP.md](SETUP.md) 참고**

---

## Phase 1. 나이브한 구현 + 베이스라인 확보

> "최적화 없이 구현하면 수치가 어떻게 나오는가?"

### 전제조건

- Phase 0 완료 (인프라 가동, 더미 데이터 삽입 완료)

### 구현 대상

- ERD 기반 엔티티 구현 (인덱스, 최적화 일절 없이)
- 베이스라인 측정용 API 구현 (최적화 없는 나이브한 코드)
- k6 부하 테스트 시나리오 작성

### 베이스라인으로 측정할 것

| API                              | 측정 지표              | 예상 현상                 |
| -------------------------------- | ---------------------- | ------------------------- |
| 주문 목록 조회                   | 발생 쿼리 수, 응답시간 | N+1 다발                  |
| 상품 검색 (카테고리 + 상태 필터) | 실행계획               | 풀스캔                    |
| 포인트 내역 100페이지            | p95 응답시간           | 뒤로 갈수록 급격히 느려짐 |

### 이 Phase에서 얻는 인사이트

- 아무것도 하지 않았을 때의 정확한 수치 — 이후 Phase의 "Before" 기준선
- 어디서 문제가 터지는지 직접 눈으로 확인

---

## Phase 2. 인덱스 설계 + 실행계획 분석

> "EXPLAIN ANALYZE가 보여주는 것을 읽을 수 있는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

풀스캔으로 확인된 쿼리에 인덱스를 설계하고, 실행계획이 바뀌는 것을 직접 확인한다.

### 단계별 실험

| #   | 실험                      | 방법                                                             | 관찰할 것                         |
| --- | ------------------------- | ---------------------------------------------------------------- | --------------------------------- |
| 1   | **풀스캔 확인**           | `EXPLAIN ANALYZE SELECT * FROM product WHERE status = 'ON_SALE'` | Seq Scan 확인                     |
| 2   | **단일 인덱스 추가**      | `CREATE INDEX idx_product_status ON product(status)`             | Index Scan으로 변경 여부          |
| 3   | **복합 인덱스 순서 함정** | `(category_id, status)` vs `(status, category_id)` 비교          | 쿼리 패턴에 따른 성능 차이        |
| 4   | **커버링 인덱스**         | SELECT 컬럼을 인덱스에 포함                                      | Index Only Scan 달성              |
| 5   | **Soft Delete 함정**      | `is_deleted = false` 조건의 인덱스 효과 측정                     | 부분 인덱스(Partial Index)로 해결 |

### 핵심 실험 쿼리

```sql
-- 실험 1: 복합 인덱스 순서
-- 쿼리 패턴: category_id 고정, status 필터
EXPLAIN ANALYZE
SELECT id, name, base_price
FROM product
WHERE category_id = 10 AND status = 'ON_SALE'
ORDER BY created_at DESC;

-- (category_id, status) 인덱스 → Index Scan ✅
-- (status, category_id) 인덱스 → 풀스캔 또는 비효율 ❌

-- 실험 2: 커버링 인덱스
-- SELECT 컬럼(id, name, base_price)을 인덱스에 포함시키면
-- 테이블 접근 없이 Index Only Scan 달성
CREATE INDEX idx_covering
  ON product(category_id, status)
  INCLUDE (name, base_price);

-- 실험 3: 부분 인덱스 (Soft Delete 함정 해결)
-- 일반 인덱스: is_deleted = false가 99%면 옵티마이저가 풀스캔 선택
-- 부분 인덱스: 처음부터 false인 행만 인덱싱
CREATE INDEX idx_product_active
  ON product(category_id, status)
  WHERE is_deleted = false;
```

### 모니터링으로 확인하는 것

- `pg_stat_statements`로 쿼리별 실행시간 Before/After 비교
- Grafana: 인덱스 추가 전후 평균 쿼리 시간 (ms)
- k6: 상품 검색 API 응답시간 분포 변화

### 이 Phase에서 얻는 인사이트

- 인덱스가 항상 빠른 게 아닌 이유 — 옵티마이저가 선택하는 기준
- 복합 인덱스는 **왼쪽 컬럼부터** 사용해야 효과가 있다
- Soft Delete 구현 방식이 인덱스 설계에 영향을 미친다

### 측정 지표 (회고용)

- 인덱스 추가 전/후 쿼리 실행시간 (ms)
- Seq Scan → Index Scan → Index Only Scan 전환 확인
- 복합 인덱스 순서 차이로 인한 성능 배율

### 남은 문제 → Phase 3으로

> "인덱스를 다 붙였는데도 주문 목록 조회가 느리다. 쿼리가 왜 이렇게 많이 나가지?"

---

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

---

## Phase 4. 트랜잭션 격리 수준

> "동시 트랜잭션에서 데이터는 어떤 시점 기준으로 보이는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

N+1을 해결하고 쿼리 수를 줄였지만, 동시에 여러 사용자가 같은 데이터에 접근하면 데이터 정합성 문제가 발생한다. 격리 수준에 따라 동일 쿼리의 읽기 결과가 달라지는 현상을 직접 실험한다.

### 단계별 실험

| #   | 실험                           | 방법                                                 | 관찰할 것                                             |
| --- | ------------------------------ | ---------------------------------------------------- | ----------------------------------------------------- |
| 1   | **격리 수준별 읽기 이상 현상** | READ COMMITTED vs REPEATABLE READ에서 동일 쿼리 실행 | Phantom Read, Non-Repeatable Read 발생 여부           |
| 2   | **격리 수준 성능 비용**        | SERIALIZABLE에서 동시 주문 처리                      | 직렬화 실패(Serialization Failure) 빈도와 처리량 변화 |

### 시나리오 1: Non-Repeatable Read (주문 중 상품 가격 변경)

```sql
-- 세션 A: 주문 생성 중 상품 가격을 두 번 조회
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT base_price FROM product WHERE id = 1;  -- 10,000원

    -- 세션 B: 이 사이에 가격 변경
    -- UPDATE product SET base_price = 15000 WHERE id = 1; COMMIT;

SELECT base_price FROM product WHERE id = 1;  -- READ COMMITTED: 15,000원 (변경됨!)
COMMIT;
-- → 같은 트랜잭션 안에서 가격이 바뀜 — 주문 금액 계산 불일치 위험

-- REPEATABLE READ로 변경하면?
BEGIN;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT base_price FROM product WHERE id = 1;  -- 10,000원

    -- 세션 B: 가격 변경 + COMMIT

SELECT base_price FROM product WHERE id = 1;  -- REPEATABLE READ: 10,000원 (스냅샷 유지)
COMMIT;
-- → 트랜잭션 시작 시점의 스냅샷을 보므로 일관성 유지
```

### 시나리오 2: Phantom Read (카테고리별 상품 수 집계 중 상품 추가)

```sql
-- 세션 A: 카테고리 10의 ON_SALE 상품 수 집계
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT COUNT(*) FROM product WHERE category_id = 10 AND status = 'ON_SALE';  -- 50건

    -- 세션 B: 새 상품 추가
    -- INSERT INTO product (category_id, status, ...) VALUES (10, 'ON_SALE', ...); COMMIT;

SELECT COUNT(*) FROM product WHERE category_id = 10 AND status = 'ON_SALE';  -- 51건 (유령 행!)
COMMIT;
-- → 집계 쿼리 결과가 달라짐 — 리포트나 통계에서 불일치 발생
```

> **PostgreSQL 특성**: PostgreSQL의 REPEATABLE READ는 MVCC 기반이라 Phantom Read도 방지한다. 이는 MySQL(InnoDB)과 다른 점이므로 직접 확인한다.

### 시나리오 3: SERIALIZABLE과 재고 차감

```java
// SERIALIZABLE에서 동시 재고 차감 — Serialization Failure 발생 실험
@Transactional(isolation = Isolation.SERIALIZABLE)
public void deductStock(Long skuId, int quantity) {
    ProductSku sku = skuRepository.findById(skuId).orElseThrow();
    if (sku.getStockQuantity() < quantity) {
        throw new IllegalStateException("재고 부족");
    }
    sku.setStockQuantity(sku.getStockQuantity() - quantity);
}

// 테스트: 10 스레드 동시 실행 시 Serialization Failure 발생 빈도 측정
// → PostgreSQL은 "could not serialize access" 에러를 던짐
// → 재시도 로직이 없으면 주문 실패로 이어짐
```

### 격리 수준별 비교표 (직접 측정)

| 격리 수준       | Non-Repeatable Read | Phantom Read | 동시 재고 차감 RPS | Serialization Failure 빈도 |
| --------------- | ------------------- | ------------ | ------------------ | -------------------------- |
| READ COMMITTED  | 발생                | 발생         | 높음               | -                          |
| REPEATABLE READ | 방지                | 방지 (PG)    | 중간               | -                          |
| SERIALIZABLE    | 방지                | 방지         | 측정               | 측정                       |

### 모니터링으로 확인하는 것

- k6: 격리 수준별 동시 주문 처리 RPS 및 에러율 비교
- Grafana: 격리 수준별 Serialization Failure 빈도 추이
- `pg_stat_statements`: 격리 수준별 쿼리 실행시간 차이

### 이 Phase에서 얻는 인사이트

- PostgreSQL의 MVCC가 격리 수준을 어떻게 구현하는지 — 락이 아닌 스냅샷 기반
- READ COMMITTED가 기본값인 이유 — 대부분의 웹 애플리케이션에서 충분한 이유와 부족한 경우
- SERIALIZABLE은 정합성은 완벽하지만 **재시도 로직 필수** — 이전 동시성 프로젝트의 낙관적 락과 유사한 패턴

### 측정 지표 (회고용)

- 격리 수준별 동시 처리 RPS 차이
- SERIALIZABLE에서의 Serialization Failure 발생률 (N건 / 총 요청)

### 남은 문제 → Phase 5로

> "데이터 정합성은 확보했는데, 필요 없는 컬럼까지 다 가져온다. 그리고 검색 조건이 동적으로 바뀌면 JPQL로는 한계가 있다."

---

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

## Phase 6. 집계 쿼리 최적화

> "GROUP BY 쿼리에서 인덱스가 어떻게 작동하는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

DTO Projection과 동적 쿼리로 단순 조회는 최적화했지만, 통계/리포트성 집계 쿼리는 별도의 최적화가 필요하다. GROUP BY에 인덱스가 미치는 영향을 실험하고, 집계 쿼리 전용 최적화 전략을 학습한다.

### 실험 1: 카테고리별 상품 통계

```sql
-- 카테고리별 상품 수 + 평균 가격
EXPLAIN ANALYZE
SELECT c.name, COUNT(p.id), AVG(p.base_price)
FROM category c
LEFT JOIN product p ON p.category_id = c.id
WHERE p.status = 'ON_SALE'
GROUP BY c.id, c.name;

-- 인덱스 없이: HashAggregate + Seq Scan
-- 인덱스 추가 후: GroupAggregate + Index Scan 가능 여부 확인
```

### 실험 2: 유저 등급별 월 구매 통계

```sql
-- 유저 등급별 월 구매 통계
EXPLAIN ANALYZE
SELECT u.grade, DATE_TRUNC('month', o.created_at), COUNT(o.id), SUM(o.final_price)
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY 2 DESC;

-- GROUP BY에 함수(DATE_TRUNC)가 포함되면 인덱스 활용이 달라짐
-- 표현식 인덱스(Expression Index) 적용 실험
```

### 실험 3: 상품별 리뷰 평점 + 리뷰 수

```sql
-- 상품별 리뷰 평점 + 리뷰 수
EXPLAIN ANALYZE
SELECT p.name, ROUND(AVG(r.rating), 2), COUNT(r.id)
FROM product p
LEFT JOIN review r ON r.product_id = p.id
GROUP BY p.id, p.name
HAVING COUNT(r.id) >= 10
ORDER BY AVG(r.rating) DESC;

-- HAVING 절이 실행계획에 미치는 영향
-- review(product_id) 인덱스가 GROUP BY 성능에 미치는 영향
```

### 실험 4: 성별/연령대별 구매 분석

```sql
-- 성별별 구매 통계 (ERD에 추가된 gender, birth_date 활용)
SELECT u.gender,
       EXTRACT(YEAR FROM AGE(u.birth_date)) / 10 * 10 AS age_group,
       COUNT(o.id), AVG(o.final_price)
FROM users u
JOIN orders o ON o.user_id = u.id
GROUP BY u.gender, age_group
ORDER BY age_group;
```

### 모니터링으로 확인하는 것

- `EXPLAIN ANALYZE`: HashAggregate vs GroupAggregate 실행계획 변화
- 집계 쿼리 인덱스 적용 전/후 실행시간 비교
- 표현식 인덱스 추가 전/후 DATE_TRUNC 집계 성능 변화

### 이 Phase에서 얻는 인사이트

- GROUP BY에서 인덱스가 활용되려면 어떤 조건이 필요한가
- HashAggregate vs GroupAggregate — 각각 언제 선택되는가
- 표현식 인덱스(Expression Index)가 필요한 경우
- 대용량 집계 쿼리에서의 실행계획 읽기

### 측정 지표 (회고용)

- 집계 쿼리 인덱스 적용 전/후 실행시간 (ms)
- HashAggregate → GroupAggregate 전환 시 성능 차이
- 표현식 인덱스 적용 전/후 실행시간 차이

### 남은 문제 → Phase 7로

> "데이터가 쌓일수록 목록 뒤쪽 페이지가 점점 느려진다. POINT_HISTORY 100만 건에서 1000페이지를 조회하면?"

---

## Phase 7. 페이지네이션 최적화

> "Offset 방식은 왜 뒤로 갈수록 느려지는가? Cursor는 어떻게 이걸 해결하는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

POINT_HISTORY, DELIVERY_TRACKING처럼 계속 쌓이는 테이블에서 Offset 방식의 한계를 측정하고, Cursor 방식으로 전환하여 정량적으로 비교한다.

### 왜 Offset이 느린가

```sql
-- 1페이지: POINT_HISTORY에서 10건
SELECT * FROM point_history WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 10 OFFSET 0;        -- 10건만 읽음 → 빠름

-- 1000페이지: 10,000번째부터 10건
SELECT * FROM point_history WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 10 OFFSET 9990;     -- 10,000건을 읽고 버린 뒤 10건 반환 → 느림
```

> **핵심:** OFFSET 9990이면 앞의 9990건을 전부 읽고 버린다. 페이지가 뒤로 갈수록 버리는 양이 늘어난다.

### Cursor 방식으로 전환

```sql
-- Cursor 방식: 마지막으로 읽은 id를 기준으로 다음 페이지 조회
-- 몇 페이지든 항상 인덱스에서 딱 10건만 읽음
SELECT * FROM point_history
WHERE user_id = 1
  AND id < :lastId          -- 마지막으로 읽은 id
ORDER BY id DESC
LIMIT 10;
```

### Count 쿼리 최적화

```java
// Spring Data JPA Pageable 사용 시 COUNT 쿼리가 자동 발생
// 복잡한 JOIN이 있으면 COUNT도 똑같이 JOIN → 병목

// Before: COUNT 쿼리에도 전체 JOIN 포함
Page<Order> findByUserId(Long userId, Pageable pageable);

// After: COUNT 쿼리 분리 (단순하게)
@Query(value = "SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.user.id = :userId",
       countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);
```

### 단계별 실험

| #   | 실험                                               | 측정 방법                            |
| --- | -------------------------------------------------- | ------------------------------------ |
| 1   | Offset 1페이지 vs 100페이지 vs 1000페이지 응답시간 | k6로 페이지별 p95 측정               |
| 2   | Cursor 방식 동일 조건 응답시간                     | 1페이지와 1000페이지가 동일한지 확인 |
| 3   | COUNT 쿼리 분리 전/후                              | 전체 목록 API 응답시간 비교          |

### Cursor 방식의 트레이드오프

| 항목             | Offset                  | Cursor               |
| ---------------- | ----------------------- | -------------------- |
| 성능             | 뒤로 갈수록 O(n)        | 항상 O(1)            |
| 임의 페이지 이동 | 가능 ("3페이지로 이동") | 불가능 (순차 탐색만) |
| 정렬 기준 변경   | 자유롭게 가능           | 커서 컬럼 고정 필요  |
| 구현 복잡도      | 단순                    | 상대적으로 복잡      |
| 적합한 UX        | 페이지 번호 UI          | 무한 스크롤, 더보기  |

### 모니터링으로 확인하는 것

- k6: 페이지 번호별 응답시간 그래프 (Offset은 우상향, Cursor는 수평)
- Grafana: Offset 1000페이지 vs Cursor 1000페이지 p95 응답시간 비교
- `EXPLAIN ANALYZE`: Offset의 `rows removed by filter` 수치 확인

### 이 Phase에서 얻는 인사이트

- Offset은 "건너뛰는 게 아니라 읽고 버리는 것"이라는 이해
- Cursor 방식이 무한 스크롤 UX에 적합한 기술적 이유
- COUNT 쿼리가 숨어서 병목을 만드는 패턴

### 측정 지표 (회고용)

- Offset: 1페이지(Nms) vs 1000페이지(Nms) — 배율 차이
- Cursor: 1페이지 vs 1000페이지 응답시간 (거의 동일함을 증명)
- COUNT 쿼리 분리 전/후 응답시간 차이 (ms)

---

## 전체 기술 도입 흐름 요약

| Phase | 핵심 주제          | 새로 도입하는 기술/패턴                                  | 해결하는 문제                                  | 발견하는 새 문제                               |
| ----- | ------------------ | -------------------------------------------------------- | ---------------------------------------------- | ---------------------------------------------- |
| 0     | 초기 셋팅          | Docker Compose, Faker 더미데이터, Prometheus+Grafana     | -                                              | -                                              |
| 1     | 베이스라인         | k6 부하 테스트, 나이브한 API 구현                        | -                                              | 풀스캔, N+1, 느린 페이지네이션                 |
| 2     | 인덱스 설계        | EXPLAIN ANALYZE, 복합/커버링/부분 인덱스                 | 풀스캔                                         | 인덱스 순서 함정, Soft Delete 함정             |
| 3     | N+1 + 로딩 전략    | Fetch Join, EntityGraph, BatchSize                       | 쿼리 N번 → 1~5번                                | Fetch Join + 페이징 OOM 위험                   |
| 4     | 트랜잭션 격리 수준 | 격리 수준 실험, MVCC 스냅샷                              | 동시 요청 시 데이터 정합성                     | SERIALIZABLE 재시도 로직 필수                  |
| 5     | 쿼리 최적화        | DTO Projection, QueryDSL, 벌크 연산                      | 불필요한 데이터 로딩, 동적 쿼리                | 집계 쿼리 + 인덱스 관계                        |
| 6     | 집계 쿼리          | GROUP BY 인덱스, 표현식 인덱스, HashAggregate 분석       | 집계 쿼리 성능                                 | 대용량 이력 테이블 페이지네이션                |
| 7     | 페이지네이션       | Cursor 페이지네이션, Count 쿼리 분리                     | Offset 성능 저하                               | Cursor 정렬 제약, 구현 복잡도                  |

---

## 학습 완료 시 답할 수 있는 질문들

> "인덱스 만들었는데 왜 안 타요?"
> → Phase 2에서 복합 인덱스 컬럼 순서 함정과 Soft Delete 무력화를 직접 측정했습니다. `EXPLAIN ANALYZE`로 Seq Scan이 선택된 이유를 확인했습니다.

> "트랜잭션 격리 수준이 뭔가요? 실무에서 뭘 써야 하나요?"
> → Phase 4에서 READ COMMITTED와 REPEATABLE READ에서 주문 중 가격 변경 시나리오를 직접 실험했습니다. PostgreSQL의 MVCC가 락 없이 스냅샷으로 격리하는 방식을 확인했고, SERIALIZABLE의 성능 비용(Serialization Failure 발생률 N%)을 k6로 측정했습니다.

> "N+1이 뭔가요? 어떻게 해결하나요?"
> → Phase 3에서 주문 100건 조회 시 쿼리가 401번 나가는 것을 직접 확인했습니다. Fetch Join, BatchSize 각각 적용 후 쿼리 수와 응답시간을 측정했습니다.

> "Fetch Join 쓰면 되지 않나요?"
> → Fetch Join과 페이징을 같이 쓰면 Hibernate가 전체 데이터를 메모리에 올립니다. Phase 3에서 경고 로그와 메모리 사용량을 직접 확인했고, BatchSize로 해결했습니다.

> "QueryDSL 왜 쓰나요? JPQL로 안 되나요?"
> → Phase 5에서 동적 검색 조건 8가지를 JPQL 문자열로 조합하다 런타임 에러를 경험했습니다. QueryDSL은 컴파일 타임에 오류를 잡아줍니다.

> "GROUP BY 쿼리가 느린데 인덱스를 어떻게 걸어야 하나요?"
> → Phase 6에서 HashAggregate와 GroupAggregate의 차이를 실행계획으로 확인했습니다. GROUP BY 컬럼에 인덱스를 추가했을 때 실행시간이 N% 개선되었고, DATE_TRUNC 같은 함수가 포함되면 표현식 인덱스가 필요함을 확인했습니다.

> "페이지네이션에서 왜 뒤로 갈수록 느려지나요?"
> → Phase 7에서 OFFSET은 앞 데이터를 전부 읽고 버리는 방식임을 `EXPLAIN ANALYZE`로 확인했습니다. 1페이지 Nms vs 1000페이지 Nms로 직접 측정했습니다.

> "Cursor 페이지네이션이 뭔가요?"
> → 마지막으로 읽은 id를 기준으로 다음 데이터를 조회하는 방식입니다. Phase 7에서 1페이지와 1000페이지 응답시간이 동일함을 k6로 증명했습니다.
