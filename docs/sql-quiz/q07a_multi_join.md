# Q07a. 다중 JOIN (4테이블+)

## 난이도
중급

## 주제
다중 INNER JOIN

## 문제

사용자의 리뷰 정보를 조회하세요.
`users`, `review`, `order_item`, `product` 테이블을 조인하여
각 리뷰의 작성자, 상품명, 리뷰 평점, 내용을 가져오세요.

- 평점(`rating`) 5점인 리뷰만 조회
- 최신 리뷰순(`review.created_at` 내림차순) 정렬

출력 컬럼:
- `review_id`
- `reviewer_name` : 작성자 이름
- `product_name` : 상품명 (product 테이블의 name)
- `rating`
- `content`
- `created_at`

## 기대 결과 형태

| review_id | reviewer_name | product_name | rating | content          | created_at          |
|-----------|---------------|--------------|--------|------------------|---------------------|
| 15        | 홍길동        | 기본 티셔츠  | 5      | 정말 좋아요!      | 2024-11-20 10:00:00 |
| 8         | 이영희        | 린넨 셔츠   | 5      | 재구매 의사 있음  | 2024-11-15 09:30:00 |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    r.id            AS review_id,
    u.name          AS reviewer_name,
    p.name          AS product_name,
    r.rating,
    r.content,
    r.created_at
FROM review r
INNER JOIN users u        ON r.user_id = u.id
INNER JOIN order_item oi  ON r.order_item_id = oi.id
INNER JOIN product_sku ps ON oi.sku_id = ps.id
INNER JOIN product p      ON ps.product_id = p.id
WHERE r.rating = 5
ORDER BY r.created_at DESC;
```

## 해설

- 이 쿼리의 JOIN 경로:
  ```
  review
    └─ users         (r.user_id = u.id)
    └─ order_item    (r.order_item_id = oi.id)
         └─ product_sku  (oi.sku_id = ps.id)
              └─ product  (ps.product_id = p.id)
  ```
- `review` 에는 `product_id` 가 직접 있지만, 여기서는 `order_item → product_sku → product` 경로로 조인하는 연습.
  - 실제로는 `review.product_id` 를 직접 JOIN 하는 것이 더 단순하다.
- 테이블이 많아지면 별칭(`r`, `u`, `oi`, `ps`, `p`)을 간결하게 유지하는 것이 중요.

> **팁:** JOIN 경로를 먼저 그림으로 그려보고 쿼리를 작성하면 실수가 줄어든다.
