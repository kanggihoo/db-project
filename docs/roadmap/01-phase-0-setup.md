# 이커머스 DB 최적화 학습 로드맵

## Phase 0. 프로젝트 초기 셋팅

> 모든 Phase의 전제조건. 최초 1회 실행하면 Docker 볼륨으로 유지된다.

- 인프라 구성, 의존성 설정, 스키마 생성, 더미 데이터 삽입
- **상세 가이드: [SETUP.md](../../SETUP.md) 참고**

### 관련 문서

- 작업 계획: [phases/00-setup/plan.md](../phases/00-setup/plan.md)
- 구현 기록: [phases/00-setup/implementation.md](../phases/00-setup/implementation.md)
- 환경 가이드: [guides/environment.md](../guides/environment.md)
- 시딩 가이드: [guides/seed-data.md](../guides/seed-data.md)
- Spring profile 가이드: [guides/spring-profiles.md](../guides/spring-profiles.md)

### 완료 조건

- [ ] Docker Compose로 PostgreSQL, Prometheus, Grafana가 기동된다.
- [ ] `docker/postgres/init.sql` 기준으로 스키마가 생성된다.
- [ ] seeder 프로필로 더미 데이터 삽입을 실행할 수 있다.
- [ ] 핵심 테이블의 row count를 확인하고 `SETUP.md` 기준과 비교했다.
- [ ] Phase 1에서 사용할 API/테스트 실행 전제 조건이 준비됐다.

---
