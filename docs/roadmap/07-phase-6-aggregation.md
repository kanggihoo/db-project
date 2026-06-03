# 이커머스 DB 최적화 학습 로드맵

## Phase 6. 집계 쿼리 최적화

> "GROUP BY 집계 쿼리에서 인덱스는 scan, join, sort, aggregate plan을 어떻게 바꾸는가?"

### 이전 Phase의 문제를 어떻게 확장하는가

Phase 5는 단순 상품 검색 read path에서 엔티티 조회와 QueryDSL DTO projection을 비교했다. Phase 6은 단순 조회가 아니라 통계/리포트성 집계 쿼리를 다룬다.

Phase 6의 목표는 인덱스 유무 자체를 다시 학습하는 것이 아니다. Phase 2에서 다룬 단순 predicate 인덱스 지식을 바탕으로, 집계 쿼리에서 baseline condition, naive index condition, query-shaped index condition을 비교한다. 이를 통해 인덱스가 `Seq Scan`, `Index Scan`, `Bitmap Scan`, `Sort`, `HashAggregate`, `GroupAggregate` 선택에 어떤 영향을 주는지 확인한다.

### 측정 원칙

Phase 6의 primary evidence는 `psql`에서 실행한 SQL-only `EXPLAIN (ANALYZE, BUFFERS)` 결과다. SQL-only evidence는 DB 내부 실행계획과 실행시간을 직접 비교하기 위한 기준이다.

k6와 Grafana는 representative API evidence로만 사용한다. 모든 SQL-only condition을 HTTP 부하 테스트로 반복하지 않는다. Product Review Summary API 하나를 대상으로 naive index condition과 query-shaped index condition만 비교해, 쿼리 plan 차이가 API 응답 시간에도 반영되는지 확인한다.

측정 전에는 데이터 규모와 분포를 evidence로 기록한다. 집계 쿼리는 row 수와 분포에 따라 planner 선택이 달라지므로, 결과 해석은 data profile과 함께 남긴다.

### Measurement Condition

| 항목 | 값 |
|---|---|
| DB | docker compose PostgreSQL |
| 데이터 | `./scripts/seed.sh loadtest` |
| SQL 실행 | `docker compose exec postgres psql -U app -d ecommerce` |
| Primary evidence | `EXPLAIN (ANALYZE, BUFFERS)` |
| API evidence | Product Review Summary API의 k6 naive/query-shaped 비교 |
| Baseline 의미 | custom 실험 인덱스가 없는 seed 직후 상태. PK/UNIQUE 기본 인덱스는 제외하지 않는다. |

### Data Profile Evidence

Phase 6 측정 전 아래 정보를 `docs/evidence/phase-06/data-profile/` 아래에 저장한다.

- `product`, `review`, `orders`, `users` row count
- review가 있는 Product 수
- Product별 review count의 min, max, avg, 상위 20개
- 월별 Order count와 매출 분포
- 측정 전 `product`, `review`, `orders`, `users` index 목록

예상 파일:

```text
docs/evidence/phase-06/data-profile/
  row-counts.txt
  review-distribution.txt
  monthly-order-distribution.txt
  index-state-before.txt
```

### SQL Script Structure

Phase 6 SQL-only evidence는 번호가 붙은 SQL 파일로 재현한다. 파일은 condition별 `prepare`와 `explain`을 분리한다.

예상 위치:

```text
scripts/phase-06/
  00-data-profile.sql
  10-review-baseline-prepare.sql
  11-review-baseline-explain.sql
  12-review-naive-prepare.sql
  13-review-naive-explain.sql
  14-review-query-shaped-prepare.sql
  15-review-query-shaped-explain.sql
  20-monthly-baseline-prepare.sql
  21-monthly-baseline-explain.sql
  22-monthly-naive-prepare.sql
  23-monthly-naive-explain.sql
  24-monthly-query-shaped-prepare.sql
  25-monthly-query-shaped-explain.sql
```

`prepare` 파일은 해당 condition을 독립적으로 재현할 수 있게 만든다.

- 같은 실험 target의 custom index를 먼저 제거한다.
- 필요한 index를 생성한다.
- 대상 테이블에 `VACUUM (ANALYZE)`를 실행해 planner 통계를 갱신한다.
- `pg_stat_statements_reset()`을 실행해 condition별 누적 통계를 분리한다.

`explain` 파일은 `EXPLAIN (ANALYZE, BUFFERS)` 대상 SQL만 담는다.

실행 예시:

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/12-review-naive-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/13-review-naive-explain.sql > docs/evidence/phase-06/review-aggregate/naive-index/explain.txt
```

`VACUUM`은 transaction block 안에서 실행할 수 없으므로 `psql -f`로 독립 실행한다. `pg_stat_statements_reset()`은 docker compose PostgreSQL에 `pg_stat_statements` extension이 활성화되어 있다는 Phase 0 조건을 전제로 한다.

### 실험 1: Product Review Summary

`Review`는 aggregation optimization target이다. Product별 리뷰 수와 평균 평점을 집계하면서 `GROUP BY`, `HAVING`, `ORDER BY AVG(...)`가 함께 있을 때 인덱스 설계가 실행계획에 어떤 영향을 주는지 확인한다.

대표 SQL:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id,
       p.name,
       ROUND(AVG(r.rating), 2) AS avg_rating,
       COUNT(r.id) AS review_count
FROM product p
LEFT JOIN review r ON r.product_id = p.id
GROUP BY p.id, p.name
HAVING COUNT(r.id) >= 10
ORDER BY AVG(r.rating) DESC, p.id ASC
LIMIT 100;
```

비교 condition:

| Condition | Index | 목적 |
|---|---|---|
| baseline | custom 실험 인덱스 없음 | 집계 쿼리의 기준 plan과 실행시간 고정 |
| naive index | `CREATE INDEX idx_review_product_id ON review(product_id);` | FK/join 컬럼 인덱스가 집계에 주는 효과 확인 |
| query-shaped index | `CREATE INDEX idx_review_product_rating ON review(product_id, rating);` | join key와 aggregate input을 함께 둔 인덱스가 plan에 주는 효과 확인 |

확인할 질문:

- `review` scan 방식이 어떻게 바뀌는가?
- `product` join 방식이 어떻게 바뀌는가?
- `HAVING COUNT(r.id) >= 10`은 인덱스로 충분히 줄어드는가?
- `ORDER BY AVG(r.rating)` 때문에 `Sort`가 남는가?
- `HashAggregate`와 `GroupAggregate` 중 어떤 aggregate plan이 선택되는가?

### 실험 2: Monthly Order Aggregate

월별 주문 집계는 함수 표현식이 포함된 `GROUP BY`를 다룬다. 일반 `created_at` 인덱스와 `DATE_TRUNC` 기반 query-shaped index가 실행계획과 실행시간을 어떻게 다르게 만드는지 확인한다.

대표 SQL:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT u.grade,
       DATE_TRUNC('month', o.created_at) AS order_month,
       COUNT(o.id) AS order_count,
       SUM(o.final_price) AS total_revenue
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY order_month DESC;
```

비교 condition:

| Condition | Index | 목적 |
|---|---|---|
| baseline | custom 실험 인덱스 없음 | 월별 집계의 기준 plan과 실행시간 고정 |
| naive index | `CREATE INDEX idx_orders_created_at ON orders(created_at);` | 시간 컬럼 단순 인덱스가 함수 기반 집계에 주는 효과 확인 |
| query-shaped index | `CREATE INDEX idx_orders_month_user ON orders ((DATE_TRUNC('month', created_at)), user_id);` | expression index가 month bucket 집계에 주는 효과 확인 |

`DATE_TRUNC` expression index는 실제 PostgreSQL에서 생성 가능 여부와 planner 사용 여부를 함께 기록한다. 생성되더라도 planner가 항상 사용한다고 가정하지 않는다.

확인할 질문:

- 일반 `orders(created_at)` 인덱스가 `DATE_TRUNC('month', created_at)` 집계에 충분한가?
- expression index가 scan, sort, aggregate plan을 바꾸는가?
- `HashAggregate`와 `GroupAggregate` 중 어떤 aggregate plan이 선택되는가?
- 실행시간 차이가 buffer 사용량 변화와 연결되는가?

### Representative API Evidence

k6 evidence는 Product Review Summary에 한정한다.

대상 API:

```http
GET /api/products/review-summary
```

이 API는 실험 1의 대표 집계 쿼리를 실행한다. Phase 6의 사용자-facing 기능 확장이 아니라, 집계 쿼리의 API-level 영향을 측정하기 위한 대표 endpoint다.

k6 비교:

| Condition | 목적 |
|---|---|
| naive index | 일반적인 FK/join 인덱스 상태의 API latency 측정 |
| query-shaped index | 쿼리 shape에 맞춘 인덱스 상태의 API latency 측정 |

baseline condition은 k6에서 반복하지 않는다. baseline은 SQL-only 기준선으로 사용하고, k6는 현실적인 후보 인덱스 두 가지가 API p95/p99에 차이를 만드는지 확인하는 보조 evidence로 둔다.

### Evidence 구조

```text
docs/evidence/phase-06/
  README.md
  data-profile/
    row-counts.txt
    review-distribution.txt
    monthly-order-distribution.txt
    index-state-before.txt
  review-aggregate/
    baseline/
      explain.txt
    naive-index/
      index-ddl.sql
      explain.txt
    query-shaped-index/
      index-ddl.sql
      explain.txt
  monthly-order-aggregate/
    baseline/
      explain.txt
    naive-index/
      index-ddl.sql
      explain.txt
    query-shaped-index/
      index-ddl.sql
      explain.txt
  review-summary-api/
    naive-index/
      k6-summary.txt
      run-window.json
    query-shaped-index/
      k6-summary.txt
      run-window.json
  grafana-screenshots/
    review-summary-naive-index.png
    review-summary-query-shaped-index.png

scripts/phase-06/
  00-data-profile.sql
  10-review-baseline-prepare.sql
  11-review-baseline-explain.sql
  12-review-naive-prepare.sql
  13-review-naive-explain.sql
  14-review-query-shaped-prepare.sql
  15-review-query-shaped-explain.sql
  20-monthly-baseline-prepare.sql
  21-monthly-baseline-explain.sql
  22-monthly-naive-prepare.sql
  23-monthly-naive-explain.sql
  24-monthly-query-shaped-prepare.sql
  25-monthly-query-shaped-explain.sql
```

### 모니터링으로 확인하는 것

- `EXPLAIN (ANALYZE, BUFFERS)`로 condition별 scan, join, sort, aggregate plan 비교
- Product Review Summary와 Monthly Order Aggregate의 baseline, naive index, query-shaped index condition별 실행시간 비교
- Monthly Order Aggregate에서 `DATE_TRUNC` expression index 전후 실행계획과 실행시간 변화
- `HashAggregate`와 `GroupAggregate` 중 어떤 plan이 선택됐는지 기록하고, 전환 여부 또는 전환되지 않은 이유 해석
- Product Review Summary API에서 naive index condition과 query-shaped index condition의 k6 p95/p99 비교

### 측정 지표

- condition별 `Planning Time`과 `Execution Time`
- condition별 shared hit/read buffer
- aggregate node 종류: `HashAggregate` 또는 `GroupAggregate`
- scan node 종류: `Seq Scan`, `Index Scan`, `Bitmap Index Scan`, `Bitmap Heap Scan`
- `Sort` 발생 여부와 `Sort Method`
- k6 representative API p95/p99, 실패율, dropped iterations
- Grafana의 Hikari active/pending/acquire time, DB table scan/index scan 추이

### 이 Phase에서 얻는 인사이트

- 집계 쿼리에서 인덱스가 항상 `GroupAggregate`를 만들지는 않는다.
- 단순 FK/join 인덱스는 join access path를 도울 수 있지만, `GROUP BY`, `HAVING`, `ORDER BY aggregate` 전체를 해결하지 못할 수 있다.
- 함수 표현식이 포함된 집계는 일반 컬럼 인덱스와 expression index의 효과가 다를 수 있다.
- 실행시간 차이는 plan node 이름만으로 판단하지 않고 buffer, row count, sort, aggregate strategy와 함께 해석해야 한다.
- API latency evidence는 DB plan evidence를 대체하지 않고, 대표 endpoint에서 최종 영향만 보조로 확인한다.

### 남은 문제 -> Phase 7

> "데이터가 쌓일수록 목록 뒤쪽 페이지가 점점 느려진다. Point History 100만 건에서 1000페이지를 조회하면?"

Phase 6은 집계 쿼리 병목을 다룬다. 대량 이력 테이블의 깊은 페이지 조회 병목은 Phase 7 pagination 실험으로 분리한다.

### 완료 조건

- [ ] `product`, `review`, `orders`, `users` row count와 review/monthly order 분포를 evidence로 기록했다.
- [ ] Phase 6 SQL-only evidence를 재현할 수 있는 번호 기반 `scripts/phase-06/*.sql` 파일을 작성했다.
- [ ] condition별 `prepare` SQL에서 custom index 상태, `VACUUM (ANALYZE)`, `pg_stat_statements_reset()` 실행 조건을 분리했다.
- [ ] Product Review Summary 집계 쿼리를 baseline, naive index, query-shaped index condition에서 비교했다.
- [ ] Monthly Order Aggregate 쿼리를 baseline, naive index, query-shaped index condition에서 비교했다.
- [ ] 각 condition의 `EXPLAIN (ANALYZE, BUFFERS)`를 `docs/evidence/phase-06/` 아래에 저장했다.
- [ ] Product Review Summary API를 naive index condition과 query-shaped index condition에서 k6로 비교했다.
- [ ] `HashAggregate`, `GroupAggregate`, `Sort`, scan 방식 변화와 execution time 차이를 report에 해석했다.
- [ ] `DATE_TRUNC` expression index의 생성 가능 여부, planner 사용 여부, 실행시간 차이를 기록했다.
- [ ] Phase 7 pagination 병목과 Phase 6 집계 쿼리 병목을 분리해 handoff를 기록했다.

---
