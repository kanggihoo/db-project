# Q10a. UPDATE 기초

## 난이도
중급

## 주제
UPDATE, 조건부 업데이트

## 문제

아래 두 가지 UPDATE를 각각 작성하세요.

### 문제 1
`users` 테이블에서 `point_balance` 가 10000 이상인 사용자의 `grade` 를 `'VIP'` 로 업데이트하세요.

### 문제 2
`product` 테이블에서 `stock_quantity` 가 0인 SKU를 가진 상품의 `status` 를 `'SOLD_OUT'` 으로 업데이트하세요.
- `product_sku` 테이블에서 `stock_quantity = 0` 인 `product_id` 를 서브쿼리로 가져와서 사용하세요.

## 실행 전 확인 (SELECT 먼저!)

UPDATE 전에 영향받을 행을 먼저 SELECT로 확인하는 습관을 기르세요.

```sql
-- 문제 1 대상 확인
SELECT id, name, grade, point_balance FROM users WHERE point_balance >= 10000;

-- 문제 2 대상 확인
SELECT id, name, status FROM product
WHERE id IN (SELECT DISTINCT product_id FROM product_sku WHERE stock_quantity = 0);
```

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- 문제 1
UPDATE users
SET grade = 'VIP'
WHERE point_balance >= 10000;

-- 문제 2
UPDATE product
SET status = 'SOLD_OUT'
WHERE id IN (
    SELECT DISTINCT product_id
    FROM product_sku
    WHERE stock_quantity = 0
);
```

## 해설

- `UPDATE 테이블 SET 컬럼 = 값 WHERE 조건` 이 기본 구조.
- **WHERE 절 없이 UPDATE 하면 전체 행이 업데이트** 된다. 항상 WHERE를 먼저 생각할 것.
- UPDATE 전 SELECT로 대상 확인 → UPDATE 실행 → 결과 SELECT로 검증 의 3단계를 습관화하자.

### 안전한 UPDATE 프로세스

```
1. SELECT로 대상 확인
2. BEGIN; (트랜잭션 시작)
3. UPDATE 실행
4. SELECT로 결과 확인
5. COMMIT; 또는 ROLLBACK;
```

> **주의:** 실제 DB에서는 실수를 되돌리기 어렵다. 트랜잭션 안에서 UPDATE 하고 결과를 확인한 뒤 COMMIT 하는 습관을 들이자.
