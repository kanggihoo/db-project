# Q14b. Running Total - SUM OVER (누적합)

## 난이도
고급

## 주제
SUM OVER, 누적합, Window Frame

## 문제

`point_history` 테이블에서 특정 사용자(`user_id = 1`)의 포인트 변동 내역을 시간순으로 조회하고,
각 행에 누적 포인트 합계를 계산하세요.

출력 컬럼:
- `id`
- `type` : EARN / USE / EXPIRE
- `amount`
- `balance_after`
- `running_total` : 해당 시점까지의 amount 누적합 (SUM OVER로 직접 계산)
- `created_at`

결과는 `created_at` 오름차순 정렬 (가장 오래된 것부터).

## 기대 결과 형태

| id | type | amount | balance_after | running_total | created_at          |
|----|------|--------|---------------|---------------|---------------------|
| 1  | EARN | 500    | 500           | 500           | 2024-09-01 10:00:00 |
| 2  | EARN | 1000   | 1500          | 1500          | 2024-09-15 14:00:00 |
| 3  | USE  | -300   | 1200          | 1200          | 2024-10-01 09:00:00 |
| 4  | EARN | 2000   | 3200          | 3200          | 2024-10-20 16:00:00 |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    id,
    type,
    amount,
    balance_after,
    SUM(amount) OVER (
        PARTITION BY user_id
        ORDER BY created_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS running_total,
    created_at
FROM point_history
WHERE user_id = 1
ORDER BY created_at;
```

## 해설

- `SUM(amount) OVER (ORDER BY created_at)` : 현재 행까지의 누적합.
- `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` : Window Frame 지정.
  - "첫 번째 행부터 현재 행까지"를 의미. `ORDER BY` 만 있을 때의 PostgreSQL 기본값과 동일.
  - 명시적으로 쓰면 의도가 명확해짐.

### Window Frame 종류

```sql
-- 첫 번째 행 ~ 현재 행 (누적합)
ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW

-- 현재 행 ~ 마지막 행 (역방향 누적)
ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING

-- 전체 파티션 (파티션 전체 합계를 모든 행에 붙임)
ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING

-- 현재 행 기준 앞뒤 N행 (이동 평균)
ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING
```

### 응용: 이동 평균 (Moving Average)

```sql
-- 최근 3건의 이동 평균
AVG(amount) OVER (
    PARTITION BY user_id
    ORDER BY created_at
    ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
)
```

> **실무 활용:** 누적 매출, 잔고 계산, 이동 평균, 전일 대비 증감 등에서 핵심적으로 사용되는 패턴.
