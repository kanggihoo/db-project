# Q06a. LEFT JOIN 기초

## 난이도
중급

## 주제
LEFT JOIN

## 문제

`users` 테이블을 기준으로 `orders` 테이블을 LEFT JOIN 하여,
**주문을 한 번도 하지 않은** 사용자 목록을 조회하세요.

출력 컬럼:
- `user_id`
- `name`
- `email`
- `grade`

## 기대 결과 형태

| user_id | name   | email              | grade  |
|---------|--------|--------------------|--------|
| 4       | 이영희 | lee@example.com    | BRONZE |
| 11      | 박민준 | park@example.com   | BRONZE |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    u.id    AS user_id,
    u.name,
    u.email,
    u.grade
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE o.id IS NULL;
```

## 해설

- `LEFT JOIN` : 왼쪽 테이블(users)의 모든 행을 유지하고, 오른쪽(orders)에 매칭되는 행이 없으면 NULL로 채운다.
- `WHERE o.id IS NULL` : 오른쪽 테이블에서 매칭된 행이 없는 경우 → 주문이 없는 사용자.

### INNER JOIN vs LEFT JOIN 비교

```
users:  A, B, C, D
orders: A, B, C (D는 주문 없음)

INNER JOIN → A, B, C (교집합)
LEFT JOIN  → A, B, C, D (users 전체 유지, D의 orders 컬럼은 NULL)
LEFT JOIN + WHERE o.id IS NULL → D만 (차집합)
```

> **실무 패턴:** "X가 없는 Y 찾기" 문제는 LEFT JOIN + IS NULL 패턴이 자주 쓰인다. (예: 리뷰 없는 상품, 장바구니에만 있고 주문 안 한 상품 등)
