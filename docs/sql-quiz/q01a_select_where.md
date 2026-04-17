# Q01a. SELECT + WHERE 기본 조건 필터

## 난이도
초급

## 주제
SELECT, WHERE

## 문제

`users` 테이블에서 아래 조건을 모두 만족하는 사용자를 조회하세요.

- 등급(`grade`)이 `'GOLD'` 또는 `'VIP'`인 사용자
- 포인트 잔액(`point_balance`)이 1000 이상인 사용자

아래 컬럼만 출력하세요.
- `id`
- `name`
- `email`
- `grade`
- `point_balance`

## 기대 결과 형태

| id | name | email | grade | point_balance |
|----|------|-------|-------|---------------|
| .. | ...  | ...   | GOLD  | 1500          |
| .. | ...  | ...   | VIP   | 3000          |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT id, name, email, grade, point_balance
FROM users
WHERE grade IN ('GOLD', 'VIP')
  AND point_balance >= 1000;
```

## 해설

- `IN ('GOLD', 'VIP')` : 여러 값 중 하나와 일치하는 조건. `grade = 'GOLD' OR grade = 'VIP'` 와 동일하지만 IN이 더 가독성이 좋다.
- `AND` : 두 조건을 모두 만족해야 행이 반환된다.
- `>=` : 이상 (1000 포함). `>` 이면 1000 초과.

> **자주 하는 실수:** `grade = 'GOLD' OR 'VIP'` 처럼 쓰면 오류. 반드시 `grade = 'GOLD' OR grade = 'VIP'` 또는 `grade IN (...)` 형태로 써야 한다.
