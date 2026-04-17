# Q08b. 서브쿼리 - EXISTS

## 난이도
중급

## 주제
서브쿼리, EXISTS, NOT EXISTS

## 문제

두 가지를 각각 조회하세요.

### 문제 1
쿠폰을 발급받았지만 **아직 사용하지 않은** 사용자 목록을 조회하세요.
`EXISTS` 를 사용하여 작성하세요.

출력 컬럼: `id`, `name`, `email`

### 문제 2
한 번도 쿠폰을 발급받지 않은 사용자 목록을 조회하세요.
`NOT EXISTS` 를 사용하여 작성하세요.

출력 컬럼: `id`, `name`, `email`, `grade`

## 기대 결과 형태

**문제 1**

| id | name   | email            |
|----|--------|------------------|
| 2  | 홍길동 | hong@example.com |

**문제 2**

| id | name   | email            | grade  |
|----|--------|------------------|--------|
| 5  | 이민지 | lee@example.com  | BRONZE |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- 문제 1: EXISTS (쿠폰 있고 미사용)
SELECT id, name, email
FROM users u
WHERE EXISTS (
    SELECT 1
    FROM user_coupon uc
    WHERE uc.user_id = u.id
      AND uc.is_used = false
);

-- 문제 2: NOT EXISTS (쿠폰 없음)
SELECT id, name, email, grade
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM user_coupon uc
    WHERE uc.user_id = u.id
);
```

## 해설

- `EXISTS (서브쿼리)` : 서브쿼리가 **1행 이상** 반환하면 true.
  - 서브쿼리 안에서 `SELECT 1` 을 관례적으로 씀 (어떤 값이든 행이 존재하는지만 확인).
  - **외부 쿼리의 컬럼을 참조** (`u.id`) → 이것이 상관 서브쿼리(Correlated Subquery).
- `NOT EXISTS` : 서브쿼리가 **0행** 반환하면 true.

### IN vs EXISTS 차이

| | IN | EXISTS |
|--|--|--|
| 동작 방식 | 서브쿼리 전체 실행 후 목록과 비교 | 조건 만족하는 행이 1개라도 있으면 즉시 true |
| NULL 처리 | IN 목록에 NULL 있으면 예상 못한 결과 가능 | 안전 |
| 권장 상황 | 서브쿼리 결과가 소량 | 대용량 데이터, NULL 포함 가능성 있을 때 |

> **EXISTS의 핵심:** "이 사용자에 대해 해당 조건을 만족하는 행이 존재하는가?" 를 묻는다. 값을 비교하는 게 아니라 **존재 여부**만 확인한다.
