# Q02b. LIMIT + OFFSET 페이지네이션

## 난이도
초급

## 주제
LIMIT, OFFSET, 페이지네이션 패턴

## 문제

`orders` 테이블에서 최근 주문순(`created_at` 내림차순)으로 정렬한 뒤,
**2페이지** 데이터를 조회하세요. (1페이지당 10건)

출력 컬럼: `id`, `user_id`, `final_price`, `status`, `created_at`

### 추가 문제
위 쿼리를 응용해서 **3페이지** 데이터를 가져오려면 어떻게 바꾸면 될까요?

## 기대 결과 형태

| id | user_id | final_price | status  | created_at          |
|----|---------|-------------|---------|---------------------|
| .. | ..      | 58000       | PAID    | 2024-11-10 14:22:00 |
| .. | ..      | 120000      | SHIPPED | 2024-11-09 09:15:00 |
(10행)

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- 2페이지 (11번째 ~ 20번째)
SELECT id, user_id, final_price, status, created_at
FROM orders
ORDER BY created_at DESC
LIMIT 10 OFFSET 10;

-- 3페이지 (21번째 ~ 30번째)
SELECT id, user_id, final_price, status, created_at
FROM orders
ORDER BY created_at DESC
LIMIT 10 OFFSET 20;
```

## 해설

- `LIMIT N` : 최대 N개 행만 반환
- `OFFSET M` : 앞에서 M개 건너뛰고 그 다음부터 반환
- 페이지네이션 공식:
  ```
  OFFSET = (페이지번호 - 1) × 페이지크기
  ```
  - 1페이지: OFFSET 0
  - 2페이지: OFFSET 10
  - 3페이지: OFFSET 20

> **실무 주의:** OFFSET 방식은 데이터가 많아질수록 느려진다. 실무에서는 `WHERE id < 마지막_id` 방식(Cursor-based pagination)을 많이 쓴다. 하지만 개념 이해는 OFFSET부터 시작하면 된다.
