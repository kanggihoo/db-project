# Q11b. DELETE + 서브쿼리

## 난이도
중급

## 주제
DELETE, 서브쿼리, FK 제약 고려

## 문제

### 문제 1
`product_image` 테이블에서 삭제된 상품(`product.is_deleted = true`)에 속한 이미지를 삭제하세요.
서브쿼리를 활용하세요.

### 문제 2
주문이 없는 빈 장바구니(`cart`)를 삭제하세요.
- `cart_item` 이 하나도 없는 `cart` 를 삭제
- FK 제약(cart_item.cart_id → cart.id) 때문에 **cart_item이 없는 것만** 삭제 가능

## 실행 전 확인

```sql
-- 문제 1 대상 확인
SELECT pi.id, pi.product_id, pi.image_url
FROM product_image pi
WHERE pi.product_id IN (SELECT id FROM product WHERE is_deleted = true);

-- 문제 2 대상 확인
SELECT c.id, c.user_id
FROM cart c
WHERE NOT EXISTS (SELECT 1 FROM cart_item ci WHERE ci.cart_id = c.id);
```

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- 문제 1
DELETE FROM product_image
WHERE product_id IN (
    SELECT id FROM product WHERE is_deleted = true
);

-- 문제 2
DELETE FROM cart
WHERE NOT EXISTS (
    SELECT 1
    FROM cart_item
    WHERE cart_id = cart.id
);
```

## 해설

- `DELETE ... WHERE IN (서브쿼리)` : 서브쿼리로 조건 대상을 가져오는 방식.
- `DELETE ... WHERE NOT EXISTS (서브쿼리)` : 관련 행이 없는 경우만 삭제.

### FK 제약과 삭제 순서

이 프로젝트 스키마에서는 FK 제약이 많다. 연관된 자식 테이블의 데이터를 먼저 삭제해야 부모 테이블을 삭제할 수 있다.

```
예: 상품 완전 삭제 순서
1. review_image, review_like (review에 의존)
2. review (order_item, product에 의존)
3. product_sku_option (product_sku, product_option_value에 의존)
4. product_option_value (product_option에 의존)
5. product_option, product_sku, product_image (product에 의존)
6. product (최종 삭제)
```

> **실무 팁:** 실제로는 `is_deleted = true` 방식의 **소프트 딜리트(Soft Delete)** 를 많이 쓴다. 실제로 삭제하지 않고 플래그만 바꿔서 FK 문제와 데이터 복구 문제를 동시에 해결.
