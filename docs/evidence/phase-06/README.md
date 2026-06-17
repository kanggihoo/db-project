# 6단계 증거

6단계 증거는 PostgreSQL 집계 쿼리에서 기준 상태, 단순 인덱스, 쿼리 형태 맞춤 인덱스 측정 조건의 실행계획과 실행시간을 비교한다.

## 데이터 프로파일

| 증거 | 경로 | 목적 |
|---|---|---|
| 통합 데이터 프로파일 | [data-profile/row-counts.txt](./data-profile/row-counts.txt) | 대상 테이블 규모, 리뷰 분포, 월별 주문 분포, 측정 전 인덱스 상태 |

## 상품 리뷰 요약

| 측정 조건 | 증거 |
|---|---|
| 기준 상태 | [review-aggregate/baseline/explain.txt](./review-aggregate/baseline/explain.txt) |
| 단순 인덱스 | [review-aggregate/naive-index/explain.txt](./review-aggregate/naive-index/explain.txt) |
| 쿼리 형태 맞춤 인덱스 | [review-aggregate/query-shaped-index/explain.txt](./review-aggregate/query-shaped-index/explain.txt) |

## 월별 주문 집계

| 측정 조건 | 증거 |
|---|---|
| 기준 상태 | [monthly-order-aggregate/baseline/explain.txt](./monthly-order-aggregate/baseline/explain.txt) |
| 단순 인덱스 | [monthly-order-aggregate/naive-index/explain.txt](./monthly-order-aggregate/naive-index/explain.txt) |
| 쿼리 형태 맞춤 인덱스 | [monthly-order-aggregate/query-shaped-index/explain.txt](./monthly-order-aggregate/query-shaped-index/explain.txt) |

## 상품 리뷰 요약 API

| 측정 조건 | k6 요약 | 실행 구간 |
|---|---|---|
| 단순 인덱스 | [review-summary-api/naive-index/k6-summary.txt](./review-summary-api/naive-index/k6-summary.txt) | [review-summary-api/naive-index/run-window.json](./review-summary-api/naive-index/run-window.json) |
| 쿼리 형태 맞춤 인덱스 | [review-summary-api/query-shaped-index/k6-summary.txt](./review-summary-api/query-shaped-index/k6-summary.txt) | [review-summary-api/query-shaped-index/run-window.json](./review-summary-api/query-shaped-index/run-window.json) |

p99 보조 증거는 [review-summary-api/percentiles-prometheus.txt](./review-summary-api/percentiles-prometheus.txt)에 기록했다.

## Grafana 스크린샷

| 측정 조건 | 증거 |
|---|---|
| 단순 인덱스 | [grafana-screenshots/review-summary-naive-index.png](./grafana-screenshots/review-summary-naive-index.png) |
| 쿼리 형태 맞춤 인덱스 | [grafana-screenshots/review-summary-query-shaped-index.png](./grafana-screenshots/review-summary-query-shaped-index.png) |
