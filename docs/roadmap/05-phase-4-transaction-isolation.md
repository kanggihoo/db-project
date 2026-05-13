# 이커머스 DB 최적화 학습 로드맵

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
