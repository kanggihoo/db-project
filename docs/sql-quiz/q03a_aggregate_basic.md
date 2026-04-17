# Q03a. 집계 함수 기초 - COUNT, SUM, AVG

## 난이도
초급

## 주제
COUNT, SUM, AVG

## 문제

`orders` 테이블과 `payment` 테이블을 활용해 아래 값을 각각 조회하세요.

### 문제 1
전체 주문 수와 완료된 결제(`payment.status = 'COMPLETED'`)의 총 결제 금액 합계, 평균 결제 금액을 조회하세요.

출력 컬럼:
- `total_orders` : 전체 주문 수
- `total_paid` : 완료된 결제 금액 합계
- `avg_paid` : 완료된 결제 평균 금액 (소수점 2자리까지)

### 문제 2
`users` 테이블에서 전화번호가 등록된 사용자 수와 등록되지 않은 사용자 수를 조회하세요.

출력 컬럼:
- `with_phone` : 전화번호 있는 사용자 수
- `without_phone` : 전화번호 없는 사용자 수

## 기대 결과 형태

**문제 1**

| total_orders | total_paid | avg_paid  |
|--------------|------------|-----------|
| 50           | 2450000    | 81666.67  |

**문제 2**

| with_phone | without_phone |
|------------|---------------|
| 15         | 5             |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- 문제 1
SELECT
    (SELECT COUNT(*) FROM orders) AS total_orders,
    SUM(amount)                   AS total_paid,
    ROUND(AVG(amount), 2)         AS avg_paid
FROM payment
WHERE status = 'COMPLETED';

-- 문제 2
SELECT
    COUNT(phone)                           AS with_phone,
    COUNT(*) - COUNT(phone)                AS without_phone
FROM users;
```

## 해설

- `COUNT(*)` : NULL 포함 전체 행 수
- `COUNT(컬럼)` : 해당 컬럼이 NULL이 아닌 행 수만 카운트
  - 이를 이용해 `COUNT(*) - COUNT(phone)` = NULL인 행 수 계산 가능
- `SUM(컬럼)` : 합계 (NULL은 무시)
- `AVG(컬럼)` : 평균 (NULL 행은 제외하고 계산)
- `ROUND(값, 자릿수)` : 반올림

> **핵심:** `COUNT(컬럼)` 과 `COUNT(*)` 의 차이를 반드시 기억하자. NULL 처리가 다르다.
