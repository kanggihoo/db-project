# Q04a. GROUP BY 그룹 집계

## 난이도
중급

## 주제
GROUP BY

## 문제

`orders` 테이블에서 **주문 상태별** 주문 건수와 총 결제 금액(`final_price`) 합계를 조회하세요.

결과는 주문 건수 내림차순으로 정렬하세요.

출력 컬럼:
- `status` : 주문 상태
- `order_count` : 해당 상태의 주문 건수
- `total_final_price` : 해당 상태의 final_price 합계

## 기대 결과 형태

| status    | order_count | total_final_price |
|-----------|-------------|-------------------|
| PAID      | 20          | 1840000           |
| DELIVERED | 15          | 1250000           |
| PENDING   | 8           | 620000            |
| ...       | ...         | ...               |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    status,
    COUNT(*)          AS order_count,
    SUM(final_price)  AS total_final_price
FROM orders
GROUP BY status
ORDER BY order_count DESC;
```

## 해설

- `GROUP BY 컬럼` : 해당 컬럼의 값이 같은 행끼리 묶어서 집계.
- `SELECT` 절에는 `GROUP BY` 에 명시된 컬럼 또는 집계 함수만 올 수 있다.
  - 예: `SELECT user_id, status, COUNT(*)` 에서 `user_id` 가 `GROUP BY status` 에 없으면 오류.
- `ORDER BY` 에서 별칭(`order_count`)을 사용할 수 있다.

> **자주 하는 실수:** GROUP BY 없이 `SELECT status, COUNT(*)` 을 쓰면 오류. 집계 함수와 일반 컬럼을 같이 SELECT 할 때는 반드시 GROUP BY가 필요하다.
