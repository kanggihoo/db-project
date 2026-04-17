# Q08a. 서브쿼리 - IN

## 난이도
중급

## 주제
서브쿼리, IN

## 문제

리뷰를 한 번이라도 작성한 사용자의 정보를 조회하세요.
**서브쿼리(IN)를 사용**하여 작성하세요. (JOIN 사용 금지)

출력 컬럼:
- `id`
- `name`
- `email`
- `grade`

결과는 `name` 오름차순으로 정렬하세요.

## 기대 결과 형태

| id | name   | email            | grade  |
|----|--------|------------------|--------|
| 3  | 김민수 | kim@example.com  | GOLD   |
| 7  | 박지영 | park@example.com | SILVER |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT id, name, email, grade
FROM users
WHERE id IN (
    SELECT DISTINCT user_id
    FROM review
)
ORDER BY name;
```

## 해설

- `WHERE id IN (서브쿼리)` : 서브쿼리가 반환하는 값 목록에 포함되는 행만 조회.
- 서브쿼리는 단독으로 실행 가능한 SELECT 문이어야 한다.
- `DISTINCT` 를 쓰면 중복 user_id 제거 (리뷰를 여러 개 써도 한 번만 포함).
  - 사실 IN에서는 중복이 있어도 결과에 영향 없지만, 명시적으로 쓰는 것이 좋은 습관.

### IN vs JOIN 비교

```sql
-- IN 서브쿼리 방식
WHERE id IN (SELECT user_id FROM review)

-- JOIN 방식 (동일한 결과)
FROM users u
INNER JOIN review r ON u.id = r.user_id
-- + DISTINCT 또는 GROUP BY u.id 필요
```

> **언제 IN을 쓰나:** 서브쿼리 결과가 단순한 값 목록일 때. 서브쿼리가 복잡하거나 데이터가 많으면 EXISTS가 더 효율적인 경우가 있다.
