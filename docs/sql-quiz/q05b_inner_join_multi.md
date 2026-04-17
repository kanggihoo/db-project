# Q05b. INNER JOIN (3테이블)

## 난이도
중급

## 주제
INNER JOIN 다중 연결

## 문제

`orders`, `order_item`, `product_sku` 테이블을 조인하여
각 주문 아이템의 상세 정보를 조회하세요.

- `orders.status = 'PAID'` 인 주문만 포함
- `order_item.quantity` 내림차순 정렬

출력 컬럼:
- `order_id`
- `product_name` : order_item의 product_name 컬럼
- `sku_code` : product_sku의 sku_code
- `quantity`
- `unit_price`

## 기대 결과 형태

| order_id | product_name | sku_code      | quantity | unit_price |
|----------|--------------|---------------|----------|------------|
| 5        | 기본 티셔츠  | SKU-001-BLK-M | 3        | 29000      |
| 12       | 린넨 셔츠    | SKU-003-WHT-L | 2        | 49000      |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    o.id          AS order_id,
    oi.product_name,
    ps.sku_code,
    oi.quantity,
    oi.unit_price
FROM orders o
INNER JOIN order_item oi ON o.id = oi.order_id
INNER JOIN product_sku ps ON oi.sku_id = ps.id
WHERE o.status = 'PAID'
ORDER BY oi.quantity DESC;
```

## 해설

- 3테이블 JOIN은 `INNER JOIN` 을 연속으로 작성한다.
  - `orders → order_item` : 주문 ↔ 주문 아이템 연결 (`o.id = oi.order_id`)
  - `order_item → product_sku` : 주문 아이템 ↔ SKU 연결 (`oi.sku_id = ps.id`)
- JOIN 순서는 논리적인 데이터 흐름에 맞게 작성하면 가독성이 좋다.

### 이 프로젝트의 주요 JOIN 관계 정리

```
users ──────── orders ──────── order_item ──────── product_sku
         user_id=id       order_id=id      sku_id=id
                                               │
                                           product
                                       product_id=id
```

> **팁:** 테이블이 많아질수록 JOIN 조건을 빠뜨리기 쉽다. 각 JOIN마다 ON 조건이 반드시 있어야 하며, 조건을 빠뜨리면 카테시안 곱(모든 행의 조합)이 발생해 엄청난 양의 결과가 나온다.
