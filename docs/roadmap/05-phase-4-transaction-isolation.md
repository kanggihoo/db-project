# 이커머스 DB 최적화 학습 로드맵

## Phase 4. 트랜잭션 격리 수준

> "동시 트랜잭션에서 데이터는 어떤 시점 기준으로 보이는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

N+1을 해결하고 쿼리 수를 줄였지만, 동시에 여러 트랜잭션이 같은 데이터에 접근하면 읽기 일관성과 동시 갱신 충돌 문제가 발생할 수 있다. 이 Phase는 비관적 락/낙관적 락/Atomic UPDATE 전략 비교가 아니라, PostgreSQL 격리 수준에 따라 동일 쿼리의 읽기 결과와 동시 갱신 실패 양상이 어떻게 달라지는지 확인하는 데 집중한다. 실제 비즈니스 동시성 제어 전략은 Phase 11에서 다룬다.

### 단계별 실험

| #   | 실험                           | 방법                                                        | 관찰할 것                                                     |
| --- | ------------------------------ | ----------------------------------------------------------- | ------------------------------------------------------------- |
| 1   | **Dirty Read 방지 확인**       | 미커밋 UPDATE를 다른 트랜잭션에서 조회                      | PostgreSQL에서는 READ UNCOMMITTED를 지정해도 Dirty Read가 없는지 |
| 2   | **격리 수준별 읽기 이상 현상** | READ COMMITTED vs REPEATABLE READ에서 동일 쿼리 반복 실행   | Non-Repeatable Read, Phantom Read 발생 여부                   |
| 3   | **Lost Update 방지 확인**      | READ COMMITTED vs REPEATABLE READ에서 naive read-modify-write 동시 실행 | PostgreSQL REPEATABLE READ가 동시 갱신을 실패시켜 Lost Update를 막는지 |

### 시나리오 1: Dirty Read (PostgreSQL에서는 발생하지 않음)

```sql
-- 세션 A: 아직 커밋하지 않은 가격 변경
BEGIN;
UPDATE product SET base_price = 15000 WHERE id = 1;

    -- 세션 B: READ UNCOMMITTED를 지정해도 PostgreSQL은 READ COMMITTED처럼 동작
    -- BEGIN;
    -- SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
    -- SELECT base_price FROM product WHERE id = 1;  -- 커밋 전 변경값을 보지 않음
    -- COMMIT;

ROLLBACK;
-- → PostgreSQL은 Dirty Read를 허용하지 않는다.
```

### 시나리오 2: Non-Repeatable Read (주문 가격 스냅샷 생성 전 live 가격 조회)

```sql
-- 세션 A: Order Item Snapshot을 만들기 전 live Product 가격을 두 번 조회
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT base_price FROM product WHERE id = 1;  -- 10,000원

    -- 세션 B: 이 사이에 가격 변경
    -- UPDATE product SET base_price = 15000 WHERE id = 1; COMMIT;

SELECT base_price FROM product WHERE id = 1;  -- READ COMMITTED: 15,000원 (변경됨!)
COMMIT;
-- → 같은 트랜잭션 안에서 live 가격 조회 결과가 바뀜

-- REPEATABLE READ로 변경하면?
BEGIN;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT base_price FROM product WHERE id = 1;  -- 10,000원

    -- 세션 B: 가격 변경 + COMMIT

SELECT base_price FROM product WHERE id = 1;  -- REPEATABLE READ: 10,000원 (스냅샷 유지)
COMMIT;
-- → 트랜잭션 시작 시점의 스냅샷을 보므로 일관성 유지
```

### 시나리오 3: Phantom Read (카테고리별 상품 수 집계 중 상품 추가)

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

### 시나리오 4: Lost Update 방지 확인

```java
// naive read-modify-write 동시 갱신 실험
// Atomic UPDATE, 비관적 락, 낙관적 락 비교가 아니라 격리 수준별 실패 양상을 관찰한다.
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void naiveAdjustStock(Long skuId, int quantity) {
    ProductSku sku = skuRepository.findById(skuId).orElseThrow();
    if (sku.getStockQuantity() < quantity) {
        throw new IllegalStateException("재고 부족");
    }
    sku.setStockQuantity(sku.getStockQuantity() - quantity);
}

// 테스트: 두 thread가 같은 Product SKU를 읽고 stale value 기반으로 갱신
// → READ COMMITTED에서는 쿼리 형태에 따라 lost update가 가능함
// → PostgreSQL REPEATABLE READ에서는 "could not serialize access due to concurrent update"로 실패할 수 있음
// → 실패 재시도나 Atomic UPDATE 전략 비교는 Phase 11 범위
```

### 격리 수준별 비교표 (측정 및 문서 비교)

| 격리 수준       | Dirty Read | Non-Repeatable Read | Phantom Read | Lost Update / 동시 갱신 충돌 |
| --------------- | ---------- | ------------------- | ------------ | ---------------------------- |
| READ COMMITTED  | 방지       | 발생                | 발생         | naive read-modify-write에서 발생 가능 |
| REPEATABLE READ | 방지       | 방지                | 방지 (PG)    | concurrent update failure로 방지 |
| SERIALIZABLE    | 미측정     | 미측정              | 미측정       | 이번 Phase 4 구현 테스트 범위 밖의 문서 비교 대상 |

### Evidence로 확인하는 것

- SQL transcript: 두 세션의 실행 순서와 조회 결과
- Integration test: 두 thread 또는 두 connection으로 순서를 고정한 재현 테스트
- server log excerpt: 필요 시 concurrent update failure 원인 로그
- `pg_stat_statements`: 필요 시 격리 수준별 실행 window를 분리해 query snapshot 저장
- k6/Grafana: 대량 부하 지표가 필요할 때만 선택적으로 사용

### 이 Phase에서 얻는 인사이트

- PostgreSQL의 MVCC가 격리 수준을 어떻게 구현하는지 — 락이 아닌 스냅샷 기반
- READ COMMITTED가 기본값인 이유 — 대부분의 웹 애플리케이션에서 충분한 이유와 부족한 경우
- PostgreSQL REPEATABLE READ는 Phantom Read뿐 아니라 같은 row 동시 갱신에 의한 Lost Update도 실패로 막는다.
- SERIALIZABLE은 커밋 성공 결과가 직렬 실행과 동등하도록 보장하지만, 충돌 시 serialization failure와 retry 비용을 동반한다.

### 측정 지표 (회고용)

- 격리 수준별 재현 결과: 발생, 방지, 실패
- concurrent update failure 발생 여부
- 선택 지표: thread test 반복 실행 시 성공/실패 건수

### 남은 문제 → Phase 5로

> "데이터 정합성은 확보했는데, 필요 없는 컬럼까지 다 가져온다. 그리고 검색 조건이 동적으로 바뀌면 JPQL로는 한계가 있다."

### 완료 조건

- [x] READ COMMITTED와 REPEATABLE READ에서 Non-Repeatable Read 차이를 재현했다.
- [x] PostgreSQL REPEATABLE READ의 Phantom Read 방지 특성을 확인했다.
- [x] PostgreSQL에서 Dirty Read가 발생하지 않음을 확인했다.
- [x] REPEATABLE READ에서 Lost Update가 조용히 발생하지 않고 concurrent update failure로 방지됨을 확인했다.
- [x] 격리 수준별 결과를 integration test evidence로 기록했다.
- [x] 락 전략 비교는 Phase 11 범위로 분리해 문서상 경계를 명확히 했다.

---
