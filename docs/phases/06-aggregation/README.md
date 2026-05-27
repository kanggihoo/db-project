# 6단계. 집계 쿼리 최적화

6단계는 PostgreSQL 집계 쿼리에서 기준 상태, 단순 인덱스, 쿼리 형태 맞춤 인덱스 측정 조건이 스캔, 조인, 정렬, 집계 실행 계획을 어떻게 바꾸는지 측정하는 학습 단계다.

## Current Status

Phase 6 is complete.

- SQL-only experiment 1: 상품 리뷰 요약 집계를 기준 상태, 단순 인덱스, 쿼리 형태 맞춤 인덱스 조건에서 비교했다.
- SQL-only experiment 2: 월별 주문 집계를 기준 상태, 단순 인덱스, 쿼리 형태 맞춤 인덱스 조건에서 비교했다.
- API comparison: Product Review Summary API를 단순 인덱스와 쿼리 형태 맞춤 인덱스 조건에서 k6/Grafana로 비교했다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | 6단계 범위와 제외 범위 |
| [runbook.md](./runbook.md) | SQL 전용 증거와 k6 증거 캡처 절차 |
| [observability.md](./observability.md) | 실행계획과 API 지표 해석 기준 |
| [report.md](./report.md) | 결과 보고서와 7단계 인계 |

## 원본 문서

- 로드맵: [docs/roadmap/07-phase-6-aggregation.md](../../roadmap/07-phase-6-aggregation.md)
- 설계 명세: [docs/superpowers/specs/2026-05-26-phase-6-aggregation-design.md](../../superpowers/specs/2026-05-26-phase-6-aggregation-design.md)
- 5단계 인계: [docs/phases/05-querydsl/report.md](../05-querydsl/report.md)

## 관련 산출물

- 단계 증거: [docs/evidence/phase-06/README.md](../../evidence/phase-06/README.md)
