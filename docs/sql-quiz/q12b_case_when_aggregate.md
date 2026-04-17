# Q12b. CASE WHEN + 집계 (조건부 집계)

## 난이도
중급

## 주제
CASE WHEN, 조건부 집계, PIVOT-like 쿼리

## 문제

`orders` 테이블에서 주문 상태별 건수를 한 행에 펼쳐서 보여주는 요약 통계를 만드세요.

출력 컬럼:
- `pending_count` : PENDING 상태 주문 수
- `paid_count` : PAID 상태 주문 수
- `shipped_count` : SHIPPED 상태 주문 수
- `delivered_count` : DELIVERED 상태 주문 수
- `cancelled_count` : CANCELLED 상태 주문 수
- `total_count` : 전체 주문 수

## 기대 결과 형태

| pending_count | paid_count | shipped_count | delivered_count | cancelled_count | total_count |
|---------------|------------|---------------|-----------------|-----------------|-------------|
| 5             | 20         | 8             | 15              | 2               | 50          |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    COUNT(CASE WHEN status = 'PENDING'   THEN 1 END) AS pending_count,
    COUNT(CASE WHEN status = 'PAID'      THEN 1 END) AS paid_count,
    COUNT(CASE WHEN status = 'SHIPPED'   THEN 1 END) AS shipped_count,
    COUNT(CASE WHEN status = 'DELIVERED' THEN 1 END) AS delivered_count,
    COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled_count,
    COUNT(*)                                          AS total_count
FROM orders;
```

## 해설

- `COUNT(CASE WHEN 조건 THEN 1 END)` : 조건이 true인 행만 카운트하는 패턴.
  - 조건이 false이면 `END` 에 `ELSE` 가 없으므로 NULL 반환 → `COUNT` 는 NULL을 무시.
  - 즉, 조건 만족하는 행만 카운트됨.
- 이 패턴을 **조건부 집계(Conditional Aggregation)** 라고 한다.
- `SUM(CASE WHEN 조건 THEN 금액 ELSE 0 END)` 으로 조건부 합계도 가능.

### SUM 방식도 동일한 결과

```sql
SELECT
    SUM(CASE WHEN status = 'PENDING'   THEN 1 ELSE 0 END) AS pending_count,
    SUM(CASE WHEN status = 'PAID'      THEN 1 ELSE 0 END) AS paid_count,
    ...
FROM orders;
```

### 응용: 상태별 금액도 같이 보기

```sql
SELECT
    COUNT(CASE WHEN status = 'PAID' THEN 1 END)            AS paid_count,
    SUM(CASE WHEN status = 'PAID' THEN final_price ELSE 0 END) AS paid_total
FROM orders;
```

> **실무 활용:** 대시보드 통계, 월별/상태별 집계 테이블 등에서 매우 자주 쓰인다. GROUP BY 없이 한 행으로 여러 조건의 집계를 뽑을 때 핵심 패턴.
