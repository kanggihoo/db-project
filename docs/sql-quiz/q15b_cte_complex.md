# Q15b. CTE 심화 - 다단계 CTE + Window Function 조합

## 난이도
고급

## 주제
다단계 CTE, Window Function 조합, 종합 문제

## 문제

**월별 매출 현황 + 전월 대비 증감**을 분석하는 쿼리를 작성하세요.

단계:
1. `orders` + `order_item` 를 조인하여 월별 총 매출 CTE 작성
2. Window Function `LAG` 를 사용해 전월 매출을 가져오는 CTE 작성
3. 최종 결과에서 증감액과 증감률 계산

출력 컬럼:
- `year_month` : `'YYYY-MM'` 형식
- `monthly_revenue` : 해당 월 총 매출
- `prev_revenue` : 전월 매출 (없으면 NULL)
- `revenue_diff` : 전월 대비 증감액 (없으면 NULL)
- `growth_rate` : 증감률 `%` (소수점 1자리, 없으면 NULL)

`year_month` 오름차순 정렬.

## 기대 결과 형태

| year_month | monthly_revenue | prev_revenue | revenue_diff | growth_rate |
|------------|-----------------|--------------|--------------|-------------|
| 2024-09    | 450000          | NULL         | NULL         | NULL        |
| 2024-10    | 780000          | 450000       | 330000       | 73.3        |
| 2024-11    | 620000          | 780000       | -160000      | -20.5       |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
WITH monthly_sales AS (
    SELECT
        TO_CHAR(o.created_at, 'YYYY-MM')      AS year_month,
        SUM(oi.unit_price * oi.quantity)       AS monthly_revenue
    FROM orders o
    INNER JOIN order_item oi ON o.id = oi.order_id
    WHERE o.status IN ('PAID', 'PREPARING', 'SHIPPED', 'DELIVERED')
    GROUP BY TO_CHAR(o.created_at, 'YYYY-MM')
),
monthly_with_lag AS (
    SELECT
        year_month,
        monthly_revenue,
        LAG(monthly_revenue) OVER (ORDER BY year_month) AS prev_revenue
    FROM monthly_sales
)
SELECT
    year_month,
    monthly_revenue,
    prev_revenue,
    monthly_revenue - prev_revenue                                 AS revenue_diff,
    ROUND(
        (monthly_revenue - prev_revenue)::NUMERIC / prev_revenue * 100,
        1
    )                                                              AS growth_rate
FROM monthly_with_lag
ORDER BY year_month;
```

## 해설

- `TO_CHAR(타임스탬프, 'YYYY-MM')` : 날짜를 원하는 형식의 문자열로 변환.
  - PostgreSQL에서 날짜 포맷팅에 사용.
- `LAG(컬럼) OVER (ORDER BY ...)` : 이전 행의 값을 가져오는 Window Function.
  - `LAG(컬럼, N)` : N번째 이전 행의 값 (기본값 1).
  - `LEAD(컬럼)` : 다음 행의 값.

### LAG / LEAD 정리

| 함수 | 설명 | 예시 |
|------|------|------|
| `LAG(col)` | 이전 행 값 | 전월 매출 |
| `LAG(col, 2)` | 2행 이전 값 | 2달 전 매출 |
| `LEAD(col)` | 다음 행 값 | 다음 달 목표치 |

- `::NUMERIC` : 정수를 실수로 캐스팅 (나눗셈 정밀도 유지).
  - `7 / 2 = 3` (정수 나눗셈), `7::NUMERIC / 2 = 3.5` (실수 나눗셈)

### 다단계 CTE 흐름

```
monthly_sales      → 월별 매출 집계
    ↓
monthly_with_lag   → LAG로 전월 매출 추가
    ↓
최종 SELECT        → 증감액/증감률 계산
```

> **종합 포인트:** 이 쿼리는 JOIN + GROUP BY + CTE + Window Function(LAG) + 날짜 함수 + 형변환을 모두 사용한다. 실무에서 매출 분석 쿼리의 전형적인 패턴이다.
