# Q13a. Window Function - ROW_NUMBER

## 난이도
고급

## 주제
Window Function, ROW_NUMBER, OVER

## 문제

`orders` 테이블에서 모든 주문을 `created_at` 내림차순으로 순번을 매기세요.
(전체 기준 순번 — 파티션 없이)

출력 컬럼:
- `row_num` : 전체 순번 (최신 주문이 1번)
- `id` : 주문 ID
- `user_id`
- `final_price`
- `created_at`

상위 15건만 출력하세요.

## 기대 결과 형태

| row_num | id | user_id | final_price | created_at          |
|---------|----|---------|-------------|---------------------|
| 1       | 50 | 3       | 189000      | 2024-11-20 14:00:00 |
| 2       | 48 | 7       | 95000       | 2024-11-19 11:30:00 |
| 3       | 47 | 1       | 45000       | 2024-11-18 09:00:00 |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    ROW_NUMBER() OVER (ORDER BY created_at DESC) AS row_num,
    id,
    user_id,
    final_price,
    created_at
FROM orders
LIMIT 15;
```

## 해설

- **Window Function (윈도우 함수)**: 행들 사이의 관계를 계산하는 함수. GROUP BY처럼 행을 합치지 않고 각 행에 개별 값을 부여.
- `ROW_NUMBER()` : 각 행에 고유한 순번을 부여. 동점이 있어도 다른 번호를 부여.
- `OVER (ORDER BY ...)` : 어떤 순서로 번호를 매길지 지정.
  - `PARTITION BY` 없이 `OVER (ORDER BY ...)` 만 쓰면 전체를 하나의 그룹으로 봄.

### Window Function 기본 구조

```sql
함수명() OVER (
    [PARTITION BY 그룹컬럼]   -- 선택: 그룹 나누기
    [ORDER BY 정렬컬럼]        -- 선택: 그룹 내 정렬
)
```

### GROUP BY와의 핵심 차이

```sql
-- GROUP BY: 그룹당 1행으로 압축
SELECT user_id, COUNT(*) FROM orders GROUP BY user_id;

-- Window Function: 원래 행 유지 + 계산값 추가
SELECT user_id, COUNT(*) OVER (PARTITION BY user_id) FROM orders;
```

> **Window Function은 SELECT 절과 ORDER BY 절에서만 사용 가능.** WHERE, HAVING에서는 쓸 수 없다. (서브쿼리로 감싸야 함)
