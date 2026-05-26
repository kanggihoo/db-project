# 6단계. 집계 쿼리 최적화

6단계는 PostgreSQL 집계 쿼리에서 기준 상태, 단순 인덱스, 쿼리 형태 맞춤 인덱스 측정 조건이 스캔, 조인, 정렬, 집계 실행 계획을 어떻게 바꾸는지 측정하는 학습 단계다.

## 현재 상태

6단계는 진행 전이다.

- 주 증거는 `psql` 기반 `EXPLAIN (ANALYZE, BUFFERS)`다.
- k6/Grafana는 상품 리뷰 요약 API의 대표 증거로만 사용한다.
- 6단계 전용 인덱스는 `scripts/phase-06/*-prepare.sql`에서만 생성/삭제한다.
- `docker/postgres/init.sql`에는 6단계 실험 인덱스를 추가하지 않는다.

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
