# Q15a. CTE 기초 (WITH절)

## 난이도
고급

## 주제
CTE (Common Table Expression), WITH절

## 문제

CTE를 사용하여 **주문 금액 상위 20% 사용자**를 찾으세요.

단계:
1. 사용자별 총 주문 금액(`total_spent`)을 구하는 CTE 작성
2. 전체 사용자의 총 주문 금액 중 상위 20% 기준값을 구하는 CTE 작성
3. 두 CTE를 활용하여 기준값 이상인 사용자 조회

출력 컬럼:
- `user_id`
- `name`
- `total_spent`
- `grade`

`total_spent` 내림차순 정렬.

## 기대 결과 형태

| user_id | name   | total_spent | grade |
|---------|--------|-------------|-------|
| 3       | 홍길동 | 720000      | VIP   |
| 7       | 이영희 | 580000      | GOLD  |
| 1       | 김철수 | 430000      | GOLD  |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
WITH user_spending AS (
    SELECT
        u.id         AS user_id,
        u.name,
        u.grade,
        COALESCE(SUM(o.final_price), 0) AS total_spent
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    GROUP BY u.id, u.name, u.grade
),
top20_threshold AS (
    SELECT PERCENTILE_CONT(0.8) WITHIN GROUP (ORDER BY total_spent) AS threshold
    FROM user_spending
)
SELECT
    user_id,
    name,
    total_spent,
    grade
FROM user_spending
WHERE total_spent >= (SELECT threshold FROM top20_threshold)
ORDER BY total_spent DESC;
```

## 해설

- **CTE (WITH절)** : 복잡한 쿼리를 단계별로 분리해서 읽기 쉽게 만드는 문법.
  ```sql
  WITH cte이름 AS (
      SELECT ...
  ),
  cte이름2 AS (
      SELECT ... FROM cte이름 ...
  )
  SELECT ... FROM cte이름2;
  ```
- 여러 CTE를 `,` 로 연결해서 순차적으로 작성 가능.
- CTE는 해당 쿼리 안에서만 유효한 임시 테이블처럼 동작.

- `PERCENTILE_CONT(0.8)` : 80번째 백분위수 값 (상위 20% 기준값).
  - `WITHIN GROUP (ORDER BY 컬럼)` 과 함께 사용.
  - `PERCENTILE_CONT(0.5)` = 중앙값(median)

### CTE vs 서브쿼리 비교

```sql
-- 서브쿼리 방식 (중첩, 읽기 어려움)
SELECT * FROM (
    SELECT user_id, SUM(final_price) AS total
    FROM orders GROUP BY user_id
) sub
WHERE total >= (SELECT AVG(total) FROM (...) sub2);

-- CTE 방식 (단계별, 읽기 쉬움)
WITH spending AS (...),
     avg_spending AS (...)
SELECT * FROM spending WHERE total >= (SELECT avg FROM avg_spending);
```

> **언제 CTE를 쓰나:** 서브쿼리가 2단계 이상 중첩되거나, 같은 서브쿼리를 여러 번 참조할 때. 가독성과 유지보수성이 크게 좋아진다.
