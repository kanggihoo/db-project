# Q14a. PARTITION BY - 그룹별 순위

## 난이도
고급

## 주제
PARTITION BY, 그룹별 순위

## 문제

`orders` 테이블에서 **사용자별로** 주문 금액(`final_price`) 순위를 구하세요.
각 사용자의 주문 중 가장 금액이 큰 주문이 1위가 되어야 합니다.

- 사용자별 주문이 2개 이상인 경우만 포함
- `user_id` 오름차순, 같은 user_id 내에서는 `rank_in_user` 오름차순 정렬

출력 컬럼:
- `user_id`
- `order_id`
- `final_price`
- `rank_in_user` : 해당 사용자 내에서의 금액 순위

## 기대 결과 형태

| user_id | order_id | final_price | rank_in_user |
|---------|----------|-------------|--------------|
| 1       | 23       | 189000      | 1            |
| 1       | 15       | 95000       | 2            |
| 1       | 8        | 45000       | 3            |
| 2       | 31       | 220000      | 1            |
| 2       | 27       | 78000       | 2            |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    user_id,
    id AS order_id,
    final_price,
    RANK() OVER (PARTITION BY user_id ORDER BY final_price DESC) AS rank_in_user
FROM orders
WHERE user_id IN (
    SELECT user_id
    FROM orders
    GROUP BY user_id
    HAVING COUNT(*) >= 2
)
ORDER BY user_id, rank_in_user;
```

## 해설

- `PARTITION BY user_id` : user_id 값이 같은 행끼리 하나의 파티션(그룹)으로 묶음.
- `ORDER BY final_price DESC` : 각 파티션 내에서 금액 기준으로 순위 결정.
- 즉, **각 사용자 안에서만 1, 2, 3...** 으로 번호를 매기고, 다음 사용자로 넘어가면 다시 1부터 시작.

### PARTITION BY 유무 비교

```sql
-- 전체 기준 순위 (PARTITION 없음)
RANK() OVER (ORDER BY final_price DESC)
-- 결과: 모든 주문 중 금액 순위

-- 사용자별 순위 (PARTITION 있음)
RANK() OVER (PARTITION BY user_id ORDER BY final_price DESC)
-- 결과: 각 사용자 내에서의 금액 순위
```

### 실무 활용 예시

| 활용 | PARTITION BY | ORDER BY |
|------|--------------|----------|
| 사용자별 최근 주문 N개 | user_id | created_at DESC |
| 카테고리별 인기 상품 순위 | category_id | 판매량 DESC |
| 월별 매출 상위 상품 | 연월 | 매출 DESC |

> **핵심 패턴:** `PARTITION BY A ORDER BY B` → "A가 같은 것들 안에서 B 기준으로 순위 매기기"
