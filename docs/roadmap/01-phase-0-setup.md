# 이커머스 DB 최적화 학습 로드맵

## Phase 0. 프로젝트 초기 셋팅

> 모든 Phase의 전제조건. 최초 1회 실행하면 Docker 볼륨으로 유지된다.

- 인프라 구성, 의존성 설정, 스키마 생성, 더미 데이터 삽입
- **상세 가이드: [SETUP.md](../../SETUP.md) 참고**

### 관련 문서

- Phase 문서 허브: [phases/00-setup/README.md](../phases/00-setup/README.md)
- 범위: [phases/00-setup/scope.md](../phases/00-setup/scope.md)
- 실행 절차: [phases/00-setup/runbook.md](../phases/00-setup/runbook.md)
- 관측 기준: [phases/00-setup/observability.md](../phases/00-setup/observability.md)
- 결과 보고서: [phases/00-setup/report.md](../phases/00-setup/report.md)
- 과거 계획: [superpowers/plans/phase-00-setup/000-legacy-plan.md](../superpowers/plans/phase-00-setup/000-legacy-plan.md)
- 환경 가이드: [guides/environment.md](../guides/environment.md)
- 시딩 가이드: [guides/seed-data.md](../guides/seed-data.md)
- Spring profile 가이드: [guides/spring-profiles.md](../guides/spring-profiles.md)

### 완료 조건

- [x] Docker Compose로 PostgreSQL, Prometheus, Grafana가 기동된다.
- [x] `docker/postgres/init.sql` 기준으로 스키마가 생성된다.
- [x] seeder 프로필로 더미 데이터 삽입을 실행할 수 있다.
- [x] 핵심 테이블의 row count를 확인하고 `SETUP.md` 기준과 비교했다.
- [x] Phase 1에서 사용할 API/테스트 실행 전제 조건이 준비됐다.

---
