# Q01c. SELECT + BETWEEN / IS NULL

## 난이도
초급

## 주제
SELECT, BETWEEN, IS NULL, IS NOT NULL

## 문제

두 가지를 각각 조회하세요.

### 문제 1
`users` 테이블에서 생일(`birth_date`)이 `1990-01-01` 부터 `1999-12-31` 사이인 사용자를 조회하세요.
- 출력 컬럼: `id`, `name`, `birth_date`, `grade`

### 문제 2
`users` 테이블에서 전화번호(`phone`)가 등록되지 않은 사용자를 조회하세요.
- 출력 컬럼: `id`, `name`, `email`, `phone`

## 기대 결과 형태

**문제 1**

| id | name | birth_date | grade  |
|----|------|------------|--------|
| .. | ...  | 1995-03-12 | SILVER |

**문제 2**

| id | name | email           | phone |
|----|------|-----------------|-------|
| .. | ...  | user@example.com | NULL  |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- 문제 1: BETWEEN
SELECT id, name, birth_date, grade
FROM users
WHERE birth_date BETWEEN '1990-01-01' AND '1999-12-31';

-- 문제 2: IS NULL
SELECT id, name, email, phone
FROM users
WHERE phone IS NULL;
```

## 해설

- `BETWEEN A AND B` : A 이상 B 이하 (양 끝값 포함). `>= A AND <= B` 와 동일.
- `IS NULL` : 값이 없는(NULL) 행 필터. **반드시 `IS NULL` 을 써야 한다.**
  - `WHERE phone = NULL` 은 항상 false → 아무 행도 안 나옴 (NULL은 어떤 값과도 `=` 비교가 안 됨)
  - `WHERE phone != NULL` 도 마찬가지로 항상 false
- `IS NOT NULL` : 값이 있는 행 필터

> **핵심 개념:** NULL은 "알 수 없는 값"이라 `=`, `!=` 로 비교할 수 없다. 반드시 `IS NULL` / `IS NOT NULL` 을 사용해야 한다.
