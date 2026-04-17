# Q04b. HAVING 그룹 필터

## 난이도
중급

## 주제
GROUP BY, HAVING

## 문제

`order_item` 테이블에서 **상품 SKU별** 총 판매 수량을 구하되,
총 판매 수량이 **5개 이상**인 SKU만 출력하세요.

결과는 총 판매 수량 내림차순으로 정렬하세요.

출력 컬럼:
- `sku_id`
- `total_quantity` : 해당 SKU의 총 판매 수량

## 기대 결과 형태

| sku_id | total_quantity |
|--------|----------------|
| 7      | 12             |
| 3      | 9              |
| 15     | 7              |
| ...    | ...            |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    sku_id,
    SUM(quantity) AS total_quantity
FROM order_item
GROUP BY sku_id
HAVING SUM(quantity) >= 5
ORDER BY total_quantity DESC;
```

## 해설

- `HAVING` : GROUP BY 이후의 집계 결과에 조건을 거는 절.
  - `WHERE` 는 그룹화 **전** 개별 행 필터
  - `HAVING` 은 그룹화 **후** 그룹 단위 필터
- SQL 실행 순서: `FROM` → `WHERE` → `GROUP BY` → `HAVING` → `SELECT` → `ORDER BY`

### WHERE vs HAVING 비교

```sql
-- WHERE: 그룹화 전 필터 (특정 주문 상태만 포함)
SELECT sku_id, SUM(quantity) AS total_quantity
FROM order_item
WHERE status = 'PENDING'   -- 개별 행 필터
GROUP BY sku_id
HAVING SUM(quantity) >= 5; -- 그룹 결과 필터
```

> **자주 하는 실수:** `HAVING COUNT(*) >= 5` 는 가능하지만 `WHERE COUNT(*) >= 5` 는 오류. WHERE 절에서는 집계 함수를 쓸 수 없다.
