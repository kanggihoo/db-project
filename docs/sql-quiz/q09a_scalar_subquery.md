# Q09a. 스칼라 서브쿼리 기초

## 난이도
중급

## 주제
스칼라 서브쿼리 (SELECT 절 서브쿼리)

## 문제

`orders` 테이블에서 각 주문 정보와 함께,
해당 주문을 한 사용자의 이름을 스칼라 서브쿼리로 가져오세요.

- `status = 'PAID'` 인 주문만 조회
- `created_at` 내림차순 정렬
- 상위 10건만 출력

출력 컬럼:
- `order_id`
- `user_name` : 스칼라 서브쿼리로 가져올 것
- `final_price`
- `status`
- `created_at`

## 기대 결과 형태

| order_id | user_name | final_price | status | created_at          |
|----------|-----------|-------------|--------|---------------------|
| 47       | 홍길동    | 189000      | PAID   | 2024-11-20 14:00:00 |
| 42       | 이영희    | 95000       | PAID   | 2024-11-18 10:30:00 |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    id AS order_id,
    (SELECT name FROM users WHERE id = orders.user_id) AS user_name,
    final_price,
    status,
    created_at
FROM orders
WHERE status = 'PAID'
ORDER BY created_at DESC
LIMIT 10;
```

## 해설

- **스칼라 서브쿼리** : SELECT 절 안에 위치하는 서브쿼리. **반드시 단 1개의 행, 1개의 컬럼**을 반환해야 한다.
  - 2개 이상의 행을 반환하면 오류 발생.
- `orders.user_id` 처럼 외부 쿼리의 컬럼을 참조 → 상관 서브쿼리.
- 매 행마다 서브쿼리가 실행되므로 데이터가 많을수록 느려진다.
  - 실무에서는 JOIN이 대부분 더 빠름. 스칼라 서브쿼리는 간단한 추가 정보를 붙일 때 유용.

### JOIN 방식과 비교

```sql
-- 스칼라 서브쿼리 방식
SELECT id, (SELECT name FROM users WHERE id = orders.user_id) AS user_name ...

-- JOIN 방식 (동일한 결과, 보통 더 빠름)
SELECT o.id, u.name AS user_name ...
FROM orders o
INNER JOIN users u ON o.user_id = u.id
```

> **팁:** 스칼라 서브쿼리는 "딱 하나의 값만 추가로 붙이고 싶을 때" 활용. 여러 컬럼이 필요하면 JOIN이 낫다.
