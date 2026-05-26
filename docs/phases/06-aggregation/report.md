# 6단계 결과 보고서

## 상태

진행 전.

## 데이터 프로파일

6단계 측정 전 행 수, 리뷰 분포, 월별 주문 분포, 인덱스 상태를 기록한다.

## 상품 리뷰 요약

| 측정 조건 | 실행계획 요약 | 실행시간 | 증거 |
|---|---|---:|---|
| 기준 상태 | 미측정 | 미측정 | `docs/evidence/phase-06/review-aggregate/baseline/explain.txt` |
| 단순 인덱스 | 미측정 | 미측정 | `docs/evidence/phase-06/review-aggregate/naive-index/explain.txt` |
| 쿼리 형태 맞춤 인덱스 | 미측정 | 미측정 | `docs/evidence/phase-06/review-aggregate/query-shaped-index/explain.txt` |

## 월별 주문 집계

| 측정 조건 | 실행계획 요약 | 실행시간 | 증거 |
|---|---|---:|---|
| 기준 상태 | 미측정 | 미측정 | `docs/evidence/phase-06/monthly-order-aggregate/baseline/explain.txt` |
| 단순 인덱스 | 미측정 | 미측정 | `docs/evidence/phase-06/monthly-order-aggregate/naive-index/explain.txt` |
| 쿼리 형태 맞춤 인덱스 | 미측정 | 미측정 | `docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt` |

## 대표 API 증거

| 측정 조건 | k6 p95 | k6 p99 | 증거 |
|---|---:|---:|---|
| 단순 인덱스 | 미측정 | 미측정 | `docs/evidence/phase-06/review-summary-api/naive-index/k6-summary.txt` |
| 쿼리 형태 맞춤 인덱스 | 미측정 | 미측정 | `docs/evidence/phase-06/review-summary-api/query-shaped-index/k6-summary.txt` |

## 7단계 인계

6단계는 집계 쿼리 병목을 다룬다. 대량 이력 테이블의 깊은 페이지 조회 병목은 7단계 페이지네이션 실험으로 분리한다.
