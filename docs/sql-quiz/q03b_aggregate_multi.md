# Q03b. 집계 함수 심화 - MIN, MAX + 조건 조합

## 난이도
초급

## 주제
MIN, MAX, COUNT, DISTINCT

## 문제

### 문제 1
`product` 테이블에서 삭제되지 않은 상품 중 가장 비싼 가격, 가장 저렴한 가격, 평균 가격을 조회하세요.

출력 컬럼:
- `max_price` : 최고가
- `min_price` : 최저가
- `avg_price` : 평균가 (소수점 없이 정수로)
- `product_count` : 상품 수

### 문제 2
`orders` 테이블에서 주문한 적 있는 **고유한** 사용자 수를 조회하세요.

출력 컬럼:
- `unique_buyers` : 고유 구매자 수

## 기대 결과 형태

**문제 1**

| max_price | min_price | avg_price | product_count |
|-----------|-----------|-----------|---------------|
| 299000    | 9900      | 68500     | 30            |

**문제 2**

| unique_buyers |
|---------------|
| 17            |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- 문제 1
SELECT
    MAX(base_price)            AS max_price,
    MIN(base_price)            AS min_price,
    ROUND(AVG(base_price))     AS avg_price,
    COUNT(*)                   AS product_count
FROM product
WHERE is_deleted = false;

-- 문제 2
SELECT COUNT(DISTINCT user_id) AS unique_buyers
FROM orders;
```

## 해설

- `MAX(컬럼)` : 최댓값
- `MIN(컬럼)` : 최솟값
- `ROUND(AVG(...))` : 소수점 없이 반올림. `ROUND(값, 0)` 과 동일.
- `COUNT(DISTINCT 컬럼)` : **중복 제거 후** 카운트.
  - 한 사용자가 주문을 여러 번 했어도 1명으로 카운트.
  - `COUNT(user_id)` 는 중복 포함이라 주문 건수가 나옴.

> **DISTINCT 위치 주의:** `DISTINCT COUNT(user_id)` 가 아니라 `COUNT(DISTINCT user_id)` 로 써야 한다. 순서가 바뀌면 문법 오류 또는 의도와 다른 결과가 나온다.
