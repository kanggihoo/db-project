# 6단계 결과 보고서

## 상태

완료.

## 결론

6단계는 `GROUP BY` 집계 쿼리에서 인덱스가 항상 같은 방식으로 효과를 내지 않는다는 점을 확인했다. 같은 "집계 쿼리"라도 상품 리뷰 요약과 월별 주문 집계는 병목 성격이 달랐다.

상품 리뷰 요약은 `LIMIT 100`이 있지만 `ORDER BY AVG(r.rating)` 때문에 먼저 전체 후보를 집계해야 한다. `review(product_id)`와 `review(product_id, rating)` 인덱스를 만들어도 `Seq Scan + HashAggregate + Sort` 계획이 유지됐고, 인덱스가 early-stop이나 정렬 생략을 만들지 못했다. 이 쿼리는 단순 B-tree 인덱스보다 사전 집계, summary table, materialized view가 더 적합한 형태다.

월별 주문 집계는 단순 `created_at` 인덱스만으로는 효과가 작았다. 하지만 `DATE_TRUNC('month', created_at)`처럼 `GROUP BY` 표현식에 맞춘 조건에서는 외부 merge sort와 temp I/O가 사라지고 병렬 집계 계획이 선택되어 실행시간이 크게 줄었다. 다만 실제 계획은 `Index Scan`이 아니라 `Parallel Seq Scan + Partial HashAggregate + Finalize GroupAggregate`였으므로, "표현식 인덱스를 직접 타서 빨라졌다"라고 단정하면 안 된다.

## 해석 기준

- `Execution Time`은 PostgreSQL 내부의 쿼리 실행시간이다.
- k6 p95와 p99는 Product Review Summary API의 HTTP 응답시간이다.
- 집계 노드 이름만으로 결론을 내리지 않는다. 버퍼, temp I/O, 정렬 방식, 스캔 방식, 병렬화 여부를 함께 본다.
- `LIMIT`은 최종 반환 행 수를 줄일 뿐, aggregate 결과로 정렬하는 쿼리에서는 입력 데이터 스캔량을 줄이지 못할 수 있다.
- API 부하 결과는 SQL 실행시간에 애플리케이션 처리, 커넥션 풀 대기, HTTP timeout이 더해진 결과다.

## 데이터 프로파일

6단계 측정 데이터는 `orders` 500,000행, `product` 100,000행, `review` 300,000행, `users` 10,000행이다. 리뷰가 있는 상품은 95,209개이고 상품별 리뷰 수는 최소 1개, 최대 12개, 평균 3.15개다. 월별 주문 분포와 측정 전 인덱스 상태는 `docs/evidence/phase-06/data-profile/row-counts.txt`에 함께 기록했다.

## 실험 가설과 실제 결과

| 대상 | 기대했던 비교 포인트 | 실제 관찰 | 결론 |
|---|---|---|---|
| 상품 리뷰 요약 | `review(product_id)`보다 `review(product_id, rating)`이 집계와 평균 평점 계산에 더 유리한지 확인 | 두 조건 모두 `Seq Scan + HashAggregate + Sort` 유지 | 이 쿼리는 현재 형태에서 인덱스 효과가 약하다. 전체 집계 후 aggregate 결과를 정렬해야 한다. |
| 월별 주문 집계 | 단순 `created_at` 인덱스보다 `DATE_TRUNC('month', created_at)` 표현식에 맞춘 조건이 유리한지 확인 | 단순 인덱스는 계획 변화가 작고, query-shaped 조건에서 병렬 집계로 전환 | `GROUP BY` 표현식에 맞춘 설계가 실행계획 선택을 바꿀 수 있다. |
| 리뷰 요약 API | SQL 인덱스 차이가 HTTP p95/p99에 반영되는지 확인 | 두 조건 모두 k6 threshold 실패, pool10 포화 | 이 API는 실시간 전체 집계 요청 경로로 두기엔 무겁다. |

## 상품 리뷰 요약

측정 SQL은 상품별 리뷰 수와 평균 평점을 계산한 뒤, 리뷰가 10개 이상인 상품 중 평균 평점이 높은 100개를 반환한다.

```sql
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

### 측정 결과

| 측정 조건 | 인덱스 조건 | 실행계획 요약 | 실행시간 | 증거 |
|---|---|---|---:|---|
| 기준 상태 | 6단계 전용 인덱스 없음 | `HashAggregate` 후 `Sort`; `review`, `product` 모두 `Seq Scan`; temp read/write 발생 | 393.093 ms | `docs/evidence/phase-06/review-aggregate/baseline/explain.txt` |
| 단순 인덱스 | `review(product_id)` | `Seq Scan + HashAggregate + Sort` 유지; 버퍼 read 감소, 계획 형태는 동일 | 324.482 ms | `docs/evidence/phase-06/review-aggregate/naive-index/explain.txt` |
| 쿼리 형태 맞춤 인덱스 | `review(product_id, rating)` | `Seq Scan + HashAggregate + Sort` 유지; 집계/정렬 병목 구조는 동일 | 326.658 ms | `docs/evidence/phase-06/review-aggregate/query-shaped-index/explain.txt` |

### 왜 `LIMIT 100`인데 인덱스 효과가 약했나

이 쿼리의 `LIMIT 100`은 읽을 데이터를 100개로 제한하지 않는다. 반환할 최종 결과만 100개로 제한한다.

상위 100개를 고르려면 `AVG(r.rating)`과 `COUNT(r.id)`가 먼저 계산되어야 한다. `AVG(r.rating)`은 개별 `review.rating` 값이 아니라 상품별 리뷰 묶음의 aggregate 결과다. PostgreSQL은 어떤 상품이 평균 평점 상위 100개인지 알기 전에 대부분의 리뷰와 상품을 조인하고 집계해야 한다.

이 구조에서는 `review(product_id)` 인덱스가 있어도 특정 상품 몇 개만 찾아오는 상황이 아니다. `review(product_id, rating)`도 마찬가지다. `rating`이 인덱스에 있어도 `ORDER BY AVG(r.rating)`은 원본 컬럼 정렬이 아니라 집계 결과 정렬이므로 인덱스 순서를 그대로 사용할 수 없다.

따라서 세 조건 모두 핵심 계획이 유지됐다.

```text
Seq Scan on review
Seq Scan on product
Hash Right Join
HashAggregate
Sort by avg(r.rating)
Limit 100
```

### 이 결과가 의미하는 것

리뷰 요약 쿼리는 "인덱스를 조심해서 만들면 GROUP BY가 빨라진다"는 메시지를 보여주는 좋은 사례가 아니다. 오히려 반대 사례다.

이 쿼리에서 얻은 교훈은 다음과 같다.

- aggregate 결과로 정렬하는 `GROUP BY` 쿼리는 `LIMIT`이 있어도 입력 스캔량이 줄지 않을 수 있다.
- `GROUP BY product_id`와 `ORDER BY AVG(rating)` 형태에서는 단순 B-tree 인덱스가 정렬을 대체하지 못한다.
- 전체 데이터 집계가 필요한 랭킹형 API는 실시간 쿼리보다 사전 집계 테이블, materialized view, 캐시가 더 적합하다.
- 리뷰 요약 API를 인덱스로 개선하려면 쿼리에 선택도 있는 조건이 필요하다. 예를 들어 최근 30일, 특정 카테고리, 특정 상품 집합처럼 먼저 후보를 줄일 수 있어야 한다.

## 월별 주문 집계

측정 SQL은 주문을 월 단위와 사용자 등급 단위로 묶어 주문 수와 매출 합계를 계산한다.

```sql
SELECT u.grade,
       DATE_TRUNC('month', o.created_at) AS order_month,
       COUNT(o.id) AS order_count,
       SUM(o.final_price) AS total_revenue
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY order_month DESC;
```

### 측정 결과

| 측정 조건 | 인덱스 조건 | 실행계획 요약 | 실행시간 | 증거 |
|---|---|---|---:|---|
| 기준 상태 | 6단계 전용 인덱스 없음 | `GroupAggregate`; `orders`, `users` `Seq Scan`; 500,000행 external merge sort | 457.079 ms | `docs/evidence/phase-06/monthly-order-aggregate/baseline/explain.txt` |
| 단순 인덱스 | `orders(created_at)` | 기준 상태와 거의 동일; `Seq Scan`과 external merge sort 유지 | 416.651 ms | `docs/evidence/phase-06/monthly-order-aggregate/naive-index/explain.txt` |
| 쿼리 형태 맞춤 인덱스 | `orders ((DATE_TRUNC('month', created_at)), user_id)` | `Finalize GroupAggregate + Partial HashAggregate`; `Parallel Seq Scan`; external merge sort와 temp I/O 제거 | 92.631 ms | `docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt` |

### 단순 `created_at` 인덱스가 약했던 이유

단순 인덱스는 `orders(created_at)`이다. 이 인덱스는 특정 기간을 찾는 쿼리에는 유리할 수 있다.

하지만 이번 쿼리는 `WHERE created_at BETWEEN ...`처럼 기간을 좁히지 않는다. 전체 `orders` 500,000행을 월 단위로 묶는다. PostgreSQL 입장에서는 인덱스에서 전체 기간을 순회하는 것보다 테이블을 순차적으로 읽고 집계하는 편이 더 싸게 평가될 수 있다.

그래서 단순 인덱스 조건에서도 핵심 병목은 유지됐다.

```text
Seq Scan on orders
Hash Join users
Sort by date_trunc('month', o.created_at), u.grade
GroupAggregate
external merge sort with temp I/O
```

### 쿼리 형태 맞춤 조건에서 달라진 점

쿼리 형태 맞춤 조건은 `GROUP BY` 표현식 자체를 인덱스 정의에 반영했다.

```sql
CREATE INDEX idx_orders_month_user
ON orders ((DATE_TRUNC('month', created_at)), user_id);
```

측정 결과 이 조건에서 PostgreSQL은 직접 `Index Scan`을 선택하지 않았다. 대신 다음 계획으로 바뀌었다.

```text
Parallel Seq Scan on orders
Hash Join users
Partial HashAggregate
Sort small grouped result
Gather Merge
Finalize GroupAggregate
```

중요한 변화는 세 가지다.

- 기준/단순 조건의 500,000행 external merge sort가 사라졌다.
- temp read/write가 사라졌다.
- 집계가 병렬화되어 worker별 `Partial HashAggregate` 후 최종 집계로 합쳐졌다.

따라서 이 결과는 "표현식 인덱스를 직접 타서 빨라졌다"가 아니다. 더 정확히는 "쿼리 형태 맞춤 조건과 최신 통계가 플래너의 집계 전략 선택을 바꿨고, 그 결과 정렬/temp I/O 병목이 제거됐다"이다.

### 이 결과가 의미하는 것

월별 주문 집계는 Phase 6의 원래 메시지를 비교적 잘 보여준다.

- `GROUP BY DATE_TRUNC(...)` 쿼리에 단순 `created_at` 인덱스만 추가하는 것은 부족할 수 있다.
- `GROUP BY`에 쓰는 표현식과 조인 키를 인덱스 설계에서 함께 고려해야 한다.
- 다만 인덱스 효과는 반드시 `Index Scan`으로만 나타나지 않는다. 병렬 집계, 정렬 제거, temp I/O 감소처럼 계획 전체의 변화로 나타날 수 있다.

## 두 집계 쿼리의 차이

| 관점 | 상품 리뷰 요약 | 월별 주문 집계 |
|---|---|---|
| 입력 축소 조건 | 없음 | 없음 |
| 정렬 기준 | `AVG(r.rating)` aggregate 결과 | `DATE_TRUNC('month', o.created_at)` grouping expression |
| `LIMIT` 영향 | 최종 반환만 제한, 집계 전 입력은 거의 줄이지 못함 | 없음 |
| 단순 인덱스 효과 | 계획 변화 없음 | 계획 변화 거의 없음 |
| 쿼리 형태 맞춤 조건 효과 | 계획 변화 없음 | 병렬 집계와 temp sort 제거 |
| 적합한 개선 방향 | summary table, materialized view, 캐시 | 표현식/집계 형태에 맞춘 인덱스와 통계 관리 |

리뷰 요약은 "랭킹을 위해 전체 후보를 계산해야 하는 집계"다. 월별 주문 집계는 "정해진 grouping expression으로 전체 데이터를 요약하는 집계"다. 둘 다 전체 데이터를 다루지만, 정렬 기준과 집계 방식이 달라 인덱스 조건이 플래너에 주는 영향도 다르게 나타났다.

## 대표 API 증거

| 측정 조건 | k6 p95 | k6 p99 | 실패율 | 증거 |
|---|---:|---:|---:|---|
| 단순 인덱스 | 9.99s | 10.32s | 6.45% | `docs/evidence/phase-06/review-summary-api/naive-index/k6-summary.txt` |
| 쿼리 형태 맞춤 인덱스 | 10s | 10.36s | 38.92% | `docs/evidence/phase-06/review-summary-api/query-shaped-index/k6-summary.txt` |

k6 두 조건 모두 20 iters/s, 5분, 최대 150 VUs 조건에서 실행했다. 단순 인덱스는 `http_req_failed` threshold를 초과했고, 쿼리 형태 맞춤 인덱스는 `http_req_duration`과 `http_req_failed` threshold를 모두 초과했다. `run-window.json`과 Grafana 캡처는 실패 종료 상태를 포함해 저장했다. p95는 k6 콘솔 요약 기준이고, p99는 `docs/evidence/phase-06/review-summary-api/percentiles-prometheus.txt`의 Prometheus histogram query 기준이다.

### 왜 20 req/s에서 threshold가 실패했나

리뷰 요약 API는 요청마다 위의 리뷰 요약 집계 SQL을 실행한다. SQL 단독 측정에서는 수백 ms 수준이지만, 부하 상황에서는 커넥션 풀 대기가 더해진다.

실제 관측값은 다음과 같다.

| 지표 | 관측값 |
|---|---:|
| Hikari max connection | 10 |
| naive-index 구간 pending max | 141 |
| query-shaped-index 구간 pending max | 150 |
| naive-index 실제 처리량 | 5128 requests / 308.7s = 약 16.6 req/s |
| query-shaped-index 실제 처리량 | 4999 requests / 308.9s = 약 16.2 req/s |

요청당 DB 점유 시간이 약 0.6초이면 pool10의 단순 계산 처리 한계는 약 16.6 req/s다.

```text
처리 한계 ~= connection 수 / 요청당 DB 점유 시간
          ~= 10 / 0.6초
          ~= 16.6 req/s
```

k6는 20 req/s를 넣으려고 했지만, 시스템은 약 16 req/s 정도만 처리했다. 나머지는 pending, timeout, dropped iteration으로 나타났다. 따라서 이 API 부하 결과는 query-shaped 인덱스가 API를 악화시켰다는 결론이 아니라, 현재 리뷰 요약 API가 pool10/20 req/s 조건에서 실시간 전체 집계 경로로 버티기 어렵다는 증거다.

## Grafana 증거

| 측정 조건 | 스크린샷 | 실행 구간 |
|---|---|---|
| 단순 인덱스 | `docs/evidence/phase-06/grafana-screenshots/review-summary-naive-index.png` | `docs/evidence/phase-06/review-summary-api/naive-index/run-window.json` |
| 쿼리 형태 맞춤 인덱스 | `docs/evidence/phase-06/grafana-screenshots/review-summary-query-shaped-index.png` | `docs/evidence/phase-06/review-summary-api/query-shaped-index/run-window.json` |

## 다음 개선 후보

리뷰 요약 API를 계속 실시간 API로 제공하려면 단순 인덱스 추가보다 구조 변경을 우선 검토한다.

- 리뷰 작성/수정 시 상품별 `review_count`, `avg_rating`을 summary table에 갱신한다.
- 주기적으로 materialized view를 refresh한다.
- 카테고리, 기간, 상품 집합 같은 선택도 있는 조건을 추가해 집계 후보를 줄인다.
- 부하 비교 목적이면 pool10/20 req/s 한계 실험과 SQL 인덱스 실험을 분리한다.

월별 주문 집계는 다음 실험에서 표현식 인덱스, 통계, 병렬 집계 설정이 각각 어떤 영향을 주는지 분리해 볼 수 있다.

## 7단계 인계

6단계는 집계 쿼리 병목을 다룬다. 대량 이력 테이블의 깊은 페이지 조회 병목은 7단계 페이지네이션 실험으로 분리한다. API k6 결과는 SQL 단독 실행시간과 다르게 애플리케이션 처리, 커넥션 대기, timeout을 함께 포함하므로 7단계에서는 DB 실행계획 증거와 HTTP 부하 증거를 계속 분리해서 해석한다.
