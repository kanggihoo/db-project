# Q13b. Window Function - RANK / DENSE_RANK

## 난이도
고급

## 주제
RANK, DENSE_RANK, ROW_NUMBER 비교

## 문제

`review` 테이블에서 상품별 평균 평점을 구하고,
평균 평점 기준으로 **RANK** 와 **DENSE_RANK** 를 모두 구하세요.

- 리뷰가 3개 이상인 상품만 포함
- 평균 평점 내림차순 정렬

출력 컬럼:
- `product_id`
- `avg_rating` : 평균 평점 (소수점 2자리)
- `review_count` : 리뷰 수
- `rnk` : RANK 함수 결과
- `dense_rnk` : DENSE_RANK 함수 결과

## 기대 결과 형태

| product_id | avg_rating | review_count | rnk | dense_rnk |
|------------|------------|--------------|-----|-----------|
| 5          | 4.80       | 5            | 1   | 1         |
| 12         | 4.50       | 4            | 2   | 2         |
| 3          | 4.50       | 3            | 2   | 2         |
| 7          | 4.00       | 6            | 4   | 3         |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    product_id,
    ROUND(AVG(rating), 2)                                          AS avg_rating,
    COUNT(*)                                                       AS review_count,
    RANK()       OVER (ORDER BY AVG(rating) DESC)                  AS rnk,
    DENSE_RANK() OVER (ORDER BY AVG(rating) DESC)                  AS dense_rnk
FROM review
GROUP BY product_id
HAVING COUNT(*) >= 3
ORDER BY avg_rating DESC;
```

## 해설

### ROW_NUMBER vs RANK vs DENSE_RANK 비교

| 값  | ROW_NUMBER | RANK | DENSE_RANK |
|-----|-----------|------|------------|
| 100 | 1         | 1    | 1          |
| 90  | 2         | 2    | 2          |
| 90  | 3         | 2    | 2          |
| 80  | 4         | 4    | 3          |

- `ROW_NUMBER` : 동점이어도 항상 다른 번호 (순서 보장 X)
- `RANK` : 동점이면 같은 번호, **다음 번호는 건너뜀** (4번이 바로 나옴)
- `DENSE_RANK` : 동점이면 같은 번호, **다음 번호는 연속** (3번이 나옴)

### Window Function에서 GROUP BY 사용 시 주의

- `OVER (ORDER BY AVG(rating) DESC)` 처럼 집계 함수를 OVER 안에서도 사용 가능.
- 이 쿼리는 먼저 GROUP BY로 집계한 후, 그 결과에 윈도우 함수가 적용된다.

> **언제 어떤 걸 쓰나:**
> - 순위에 중복 없어야 함 → `ROW_NUMBER`
> - 공동 순위 허용, 다음 순위 건너뜀 → `RANK` (스포츠 순위, 검색 결과 등)
> - 공동 순위 허용, 다음 순위 연속 → `DENSE_RANK` (등급 분류 등)
