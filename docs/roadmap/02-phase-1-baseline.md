# 이커머스 DB 최적화 학습 로드맵

## Phase 1. 나이브한 구현 + 베이스라인 확보

> "최적화 없이 구현하면 수치가 어떻게 나오는가?"

### 전제조건

- Phase 0 완료 (인프라 가동, 더미 데이터 삽입 완료)

### 관련 문서

- Phase 문서 허브: [phases/01-baseline/README.md](../phases/01-baseline/README.md)
- 범위: [phases/01-baseline/scope.md](../phases/01-baseline/scope.md)
- 실행 절차: [phases/01-baseline/runbook.md](../phases/01-baseline/runbook.md)
- 관측 전략: [phases/01-baseline/observability.md](../phases/01-baseline/observability.md)
- 결과 보고서: [phases/01-baseline/report.md](../phases/01-baseline/report.md)
- 과거 계획: [superpowers/plans/phase-01-baseline/000-legacy-plan.md](../superpowers/plans/phase-01-baseline/000-legacy-plan.md)
- k6 공통 가이드: [guides/k6-load-testing.md](../guides/k6-load-testing.md)
- Grafana 공통 가이드: [guides/grafana-observability.md](../guides/grafana-observability.md)

### 구현 대상

- ERD 기반 엔티티 구현 (인덱스, 최적화 일절 없이)
- 베이스라인 측정용 API 구현 (최적화 없는 나이브한 코드)
- k6 부하 테스트 시나리오 작성

### 베이스라인으로 측정할 것

| API                              | 측정 지표              | 예상 현상                 |
| -------------------------------- | ---------------------- | ------------------------- |
| 주문 목록 조회                   | 발생 쿼리 수, 응답시간 | N+1 다발                  |
| 상품 검색 (카테고리 + 상태 필터) | 실행계획               | 풀스캔                    |
| 포인트 내역 100페이지            | p95 응답시간           | 뒤로 갈수록 급격히 느려짐 |

### 이 Phase에서 얻는 인사이트

- 아무것도 하지 않았을 때의 정확한 수치 — 이후 Phase의 "Before" 기준선
- 어디서 문제가 터지는지 직접 눈으로 확인

### 완료 조건

- [x] 주문 목록, 상품 검색, 포인트 내역 조회 API 3종이 나이브한 방식으로 동작한다.
- [x] 주문 목록 조회에서 N+1 또는 과도한 쿼리 수를 확인했다.
- [x] 상품 검색 쿼리에서 풀스캔 또는 비효율적인 실행계획을 확인했다.
- [x] 포인트 내역 Offset 페이지네이션에서 뒤쪽 페이지 지연을 측정했다.
- [x] k6 결과와 SQL 분석 결과를 `BASELINE.md` 또는 동등한 기준선 문서에 기록했다.

---
