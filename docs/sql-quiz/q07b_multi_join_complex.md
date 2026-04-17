# Q07b. 다중 JOIN + GROUP BY 조합

## 난이도
중급

## 주제
다중 JOIN, GROUP BY, 집계

## 문제

카테고리별 매출 현황을 조회하세요.
`category`, `product`, `product_sku`, `order_item`, `orders` 를 조인하여
카테고리별 총 판매 금액(`unit_price × quantity` 합계)을 구하세요.

- `orders.status` 가 `'PAID'`, `'PREPARING'`, `'SHIPPED'`, `'DELIVERED'` 인 주문만 포함
- 총 판매 금액 내림차순 정렬

출력 컬럼:
- `category_id`
- `category_name`
- `total_revenue` : 해당 카테고리 총 판매 금액

## 기대 결과 형태

| category_id | category_name | total_revenue |
|-------------|---------------|---------------|
| 2           | 상의          | 1250000       |
| 5           | 하의          | 890000        |
| 1           | 의류          | 430000        |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    c.id                              AS category_id,
    c.name                            AS category_name,
    SUM(oi.unit_price * oi.quantity)  AS total_revenue
FROM category c
INNER JOIN product p      ON c.id = p.category_id
INNER JOIN product_sku ps ON p.id = ps.product_id
INNER JOIN order_item oi  ON ps.id = oi.sku_id
INNER JOIN orders o       ON oi.order_id = o.id
WHERE o.status IN ('PAID', 'PREPARING', 'SHIPPED', 'DELIVERED')
GROUP BY c.id, c.name
ORDER BY total_revenue DESC;
```

## 해설

- JOIN 경로:
  ```
  category → product → product_sku → order_item → orders
  ```
- `SUM(oi.unit_price * oi.quantity)` : 각 주문 아이템의 소계를 합산. 컬럼 간 연산도 집계 함수 안에서 가능.
- `GROUP BY c.id, c.name` : SELECT에 있는 비집계 컬럼은 모두 GROUP BY에 포함.
- `WHERE` 절에서 주문 상태 필터를 먼저 걸고 GROUP BY로 집계 → 취소/환불 주문은 제외.

> **실무 패턴:** 매출 집계 쿼리는 이 구조가 기본이다. 필터 조건(취소 제외, 기간 지정 등)을 WHERE 절에 추가하는 연습을 많이 해두면 좋다.
