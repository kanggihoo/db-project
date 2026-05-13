# 이커머스 DB 최적화 학습 로드맵

## Phase 6. 집계 쿼리 최적화

> "GROUP BY 쿼리에서 인덱스가 어떻게 작동하는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

DTO Projection과 동적 쿼리로 단순 조회는 최적화했지만, 통계/리포트성 집계 쿼리는 별도의 최적화가 필요하다. GROUP BY에 인덱스가 미치는 영향을 실험하고, 집계 쿼리 전용 최적화 전략을 학습한다.

### 실험 1: 카테고리별 상품 통계

```sql
-- 카테고리별 상품 수 + 평균 가격
EXPLAIN ANALYZE
SELECT c.name, COUNT(p.id), AVG(p.base_price)
FROM category c
LEFT JOIN product p ON p.category_id = c.id
WHERE p.status = 'ON_SALE'
GROUP BY c.id, c.name;

-- 인덱스 없이: HashAggregate + Seq Scan
-- 인덱스 추가 후: GroupAggregate + Index Scan 가능 여부 확인
```

### 실험 2: 유저 등급별 월 구매 통계

```sql
-- 유저 등급별 월 구매 통계
EXPLAIN ANALYZE
SELECT u.grade, DATE_TRUNC('month', o.created_at), COUNT(o.id), SUM(o.final_price)
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY 2 DESC;

-- GROUP BY에 함수(DATE_TRUNC)가 포함되면 인덱스 활용이 달라짐
-- 표현식 인덱스(Expression Index) 적용 실험
```

### 실험 3: 상품별 리뷰 평점 + 리뷰 수

```sql
-- 상품별 리뷰 평점 + 리뷰 수
EXPLAIN ANALYZE
SELECT p.name, ROUND(AVG(r.rating), 2), COUNT(r.id)
FROM product p
LEFT JOIN review r ON r.product_id = p.id
GROUP BY p.id, p.name
HAVING COUNT(r.id) >= 10
ORDER BY AVG(r.rating) DESC;

-- HAVING 절이 실행계획에 미치는 영향
-- review(product_id) 인덱스가 GROUP BY 성능에 미치는 영향
```

### 실험 4: 성별/연령대별 구매 분석

```sql
-- 성별별 구매 통계 (ERD에 추가된 gender, birth_date 활용)
SELECT u.gender,
       EXTRACT(YEAR FROM AGE(u.birth_date)) / 10 * 10 AS age_group,
       COUNT(o.id), AVG(o.final_price)
FROM users u
JOIN orders o ON o.user_id = u.id
GROUP BY u.gender, age_group
ORDER BY age_group;
```

### 모니터링으로 확인하는 것

- `EXPLAIN ANALYZE`: HashAggregate vs GroupAggregate 실행계획 변화
- 집계 쿼리 인덱스 적용 전/후 실행시간 비교
- 표현식 인덱스 추가 전/후 DATE_TRUNC 집계 성능 변화

### 이 Phase에서 얻는 인사이트

- GROUP BY에서 인덱스가 활용되려면 어떤 조건이 필요한가
- HashAggregate vs GroupAggregate — 각각 언제 선택되는가
- 표현식 인덱스(Expression Index)가 필요한 경우
- 대용량 집계 쿼리에서의 실행계획 읽기

### 측정 지표 (회고용)

- 집계 쿼리 인덱스 적용 전/후 실행시간 (ms)
- HashAggregate → GroupAggregate 전환 시 성능 차이
- 표현식 인덱스 적용 전/후 실행시간 차이

### 남은 문제 → Phase 7로

> "데이터가 쌓일수록 목록 뒤쪽 페이지가 점점 느려진다. POINT_HISTORY 100만 건에서 1000페이지를 조회하면?"

### 완료 조건

- [ ] 카테고리별 상품 통계, 월 구매 통계, 리뷰 집계 중 최소 2개 집계 쿼리를 실행했다.
- [ ] 집계 쿼리의 인덱스 적용 전/후 실행계획과 실행시간을 비교했다.
- [ ] HashAggregate와 GroupAggregate 중 어떤 계획이 선택됐는지 설명할 수 있다.
- [ ] 표현식 인덱스 적용 여부에 따른 DATE_TRUNC 집계 성능 차이를 확인했다.
- [ ] 대량 이력 테이블 조회 병목을 Phase 7 페이지네이션 실험으로 연결했다.

---
