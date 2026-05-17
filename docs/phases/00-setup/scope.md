# Phase 0 범위

## 목표

Phase 0의 목표는 Ecommerce DB Optimization Lab의 재현 가능한 실험 기반을 만드는 것이다.

- Docker Compose 기반 PostgreSQL, Prometheus, Grafana, postgres-exporter 실행
- PostgreSQL DDL과 `pg_stat_statements` 준비
- 이커머스 모델 엔티티와 시딩 파이프라인 구현
- Testcontainers 기반 통합 테스트 구성
- Phase 1 이후 Baseline 측정을 위한 의도적 비최적화 상태 유지

## 대상 구성요소

| 영역 | 대상 |
|---|---|
| 인프라 | `docker-compose.yml`, PostgreSQL, Prometheus, Grafana, postgres-exporter |
| DB 스키마 | `docker/postgres/init.sql`, Layer 0~6 테이블, CHECK/FK 제약 |
| Spring 설정 | `application.yaml`, `application-seeder.yaml`, Actuator/Prometheus 설정 |
| 시딩 | `BulkInsertRepository`, `DataSeeder`, preset 기반 데이터 생성 |
| 테스트 | Testcontainers, 시딩 통합 테스트, 테스트 전용 DDL |

## 제외 범위

- Phase 0에서는 성능 최적화를 적용하지 않는다.
- FK 인덱스, 복합 인덱스, 부분 인덱스는 추가하지 않는다.
- API별 부하 테스트와 성능 해석은 Phase 1 이후에서 수행한다.
- Phase별 Grafana Evidence 캡처는 `docs/evidence/` 규칙에 따른다.

## 완료 조건

- [x] Docker Compose 인프라가 정상 기동한다.
- [x] Spring Boot 애플리케이션이 `ddl-auto=validate`를 통과한다.
- [x] `seeder` 프로필로 데이터가 삽입된다.
- [x] 주요 테이블 row count와 FK 정합성을 확인할 수 있다.
- [x] Testcontainers 기반 시딩 통합 테스트가 통과한다.

## 의도적 제약

| 제약 | 이유 |
|---|---|
| `ddl-auto=validate` 사용 | PostgreSQL 고유 DDL을 명시적으로 관리하기 위해 |
| JDBC Batch Insert 사용 | 대량 시딩에서 JPA 영속성 컨텍스트 오버헤드를 피하기 위해 |
| ID 사전 확보 | Layer 간 FK 참조를 INSERT 전에 확정하기 위해 |
| FK 인덱스 미추가 | Phase 1 Baseline과 Phase 2 인덱스 비교를 위해 |
