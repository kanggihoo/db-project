# Phase 3 결과 보고서

## 결론

Phase 3 측정 전이다. 이 문서는 Lazy, Fetch Join, BatchSize, EntityGraph 전략 evidence 수집 후 업데이트한다.

## 전략별 비교

| Strategy | SQL count/request | k6 p95 | failed | dropped | Hikari pending | Notes |
|---|---:|---:|---:|---:|---:|---|
| lazy | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | N+1 baseline |
| fetch-join | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | join row duplication 확인 |
| batch-size | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | `IN (...)` query 확인 |
| entity-graph | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | annotation 기반 graph |

## Phase 4 Handoff

조회 쿼리 수 최적화 후 남는 질문은 동시 주문/재고 변경 시 데이터 정합성과 격리 수준 문제다.
