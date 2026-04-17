# Q09b. 스칼라 서브쿼리 심화

## 난이도
중급

## 주제
스칼라 서브쿼리, 집계와 결합

## 문제

`users` 테이블에서 모든 사용자 정보와 함께,
각 사용자의 **총 주문 수**와 **가장 최근 주문일**을 스칼라 서브쿼리로 함께 조회하세요.

- 총 주문 수가 0인 사용자도 포함
- `grade` 오름차순, 그 다음 `name` 오름차순 정렬

출력 컬럼:
- `id`
- `name`
- `grade`
- `order_count` : 총 주문 수 (없으면 0)
- `last_order_date` : 가장 최근 주문일 (없으면 NULL)

## 기대 결과 형태

| id | name   | grade  | order_count | last_order_date     |
|----|--------|--------|-------------|---------------------|
| 3  | 김민수 | BRONZE | 0           | NULL                |
| 1  | 홍길동 | GOLD   | 7           | 2024-11-20 14:00:00 |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    id,
    name,
    grade,
    (SELECT COUNT(*)
     FROM orders
     WHERE user_id = users.id)           AS order_count,
    (SELECT MAX(created_at)
     FROM orders
     WHERE user_id = users.id)           AS last_order_date
FROM users
ORDER BY grade ASC, name ASC;
```

## 해설

- 스칼라 서브쿼리 안에 집계 함수(`COUNT`, `MAX`) 사용 가능.
  - `COUNT(*)` 는 행이 없으면 0 반환 → 주문 없는 사용자도 0으로 출력.
  - `MAX(created_at)` 는 행이 없으면 NULL 반환.
- 하나의 SELECT 절에 여러 개의 스칼라 서브쿼리를 넣을 수 있다.

### 대안: LEFT JOIN + GROUP BY (더 효율적)

```sql
SELECT
    u.id,
    u.name,
    u.grade,
    COUNT(o.id)       AS order_count,
    MAX(o.created_at) AS last_order_date
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name, u.grade
ORDER BY u.grade ASC, u.name ASC;
```

> **비교 포인트:** 두 방식이 같은 결과를 냄을 직접 확인해보자. 데이터가 많을수록 LEFT JOIN + GROUP BY가 빠르다.
