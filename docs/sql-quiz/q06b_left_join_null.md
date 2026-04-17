# Q06b. LEFT JOIN + NULL 활용

## 난이도
중급

## 주제
LEFT JOIN, NULL 처리, COALESCE

## 문제

`users` 테이블과 `orders` 테이블을 LEFT JOIN 하여,
모든 사용자의 주문 현황을 조회하세요.

- 주문이 없으면 `order_count` 는 `0`, `total_spent` 는 `0` 으로 출력
- `total_spent` 내림차순 정렬

출력 컬럼:
- `user_id`
- `name`
- `order_count` : 해당 사용자의 총 주문 수
- `total_spent` : 해당 사용자의 final_price 합계

## 기대 결과 형태

| user_id | name   | order_count | total_spent |
|---------|--------|-------------|-------------|
| 3       | 홍길동 | 8           | 720000      |
| 1       | 김철수 | 5           | 430000      |
| ...     | ...    | ...         | ...         |
| 4       | 이영희 | 0           | 0           |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    u.id                            AS user_id,
    u.name,
    COUNT(o.id)                     AS order_count,
    COALESCE(SUM(o.final_price), 0) AS total_spent
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name
ORDER BY total_spent DESC;
```

## 해설

- `COUNT(o.id)` : NULL인 행(주문 없는 사용자)은 카운트 안 됨 → 자동으로 0이 나옴.
  - `COUNT(*)` 를 쓰면 주문 없는 사용자도 1로 카운트되므로 주의.
- `COALESCE(값, 기본값)` : 값이 NULL이면 기본값을 반환.
  - `SUM(o.final_price)` 에서 주문이 없으면 NULL이 나오는데, `COALESCE(..., 0)` 으로 0으로 변환.
- `GROUP BY u.id, u.name` : users 기준으로 그룹화. name도 SELECT에 있으므로 GROUP BY에 포함.

> **COALESCE 활용:** `COALESCE(a, b, c)` 처럼 여러 값을 넣으면 첫 번째 NULL이 아닌 값을 반환한다. NULL 처리 시 매우 자주 쓰인다.
