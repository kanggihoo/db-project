# Phase 0. 프로젝트 셋업

Phase 0은 모든 Learning Phase의 기반을 만든다. PostgreSQL, Prometheus, Grafana, postgres-exporter를 Docker Compose로 실행하고, 이커머스 모델 스키마와 대량 시딩 파이프라인을 준비한다.

## 현재 상태

Phase 0은 완료된 상태다.

- PostgreSQL 17, Prometheus, Grafana, postgres-exporter 구성 완료
- Layer 0~6 이커머스 모델 DDL 구성 완료
- `seeder` 프로필 기반 대량 데이터 생성 파이프라인 구성 완료
- Testcontainers 기반 통합 테스트 구성 완료
- Phase 1 베이스라인 측정을 위한 의도적 제약 유지

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 0 범위와 완료 조건 |
| [runbook.md](./runbook.md) | 인프라 기동, 시딩, 검증 실행 절차 |
| [observability.md](./observability.md) | 인프라와 DB 상태 관측 기준 |
| [report.md](./report.md) | 구현 결과, 검증 결과, 남은 주의사항 |

## 관련 산출물

- 과거 구현 계획: [docs/superpowers/plans/phase-00-setup/000-legacy-plan.md](../../superpowers/plans/phase-00-setup/000-legacy-plan.md)
- 공통 환경 가이드: [docs/guides/environment.md](../../guides/environment.md)
- 시드 데이터 가이드: [docs/guides/seed-data.md](../../guides/seed-data.md)

## 다음 Phase 연결

Phase 1은 이 셋업 위에서 나이브 구현의 Baseline을 측정한다. Phase 0에서는 FK 인덱스와 최적화 설정을 의도적으로 추가하지 않는다.
