# Conventions

## 작업 규칙

- 메모리 시작점은 `.memory/README.md`이며, 이후 `project-brief.md`, `current-state.md`, `tasks.md`, `decisions.md`, `conventions.md` 순서로 읽는다.
- `docs/sql-quiz/`는 현재 메모리와 무관하므로 읽지 않는다.
- Windows PowerShell에서 한글 문서를 읽을 때는 `Get-Content -Encoding UTF8`을 사용한다.
- 세션 시작 시 전체 source code 탐색이나 테스트 실행을 하지 않는다.
- source code 확인은 메모리만으로 부족하거나 사용자가 구현/수정/검증을 요청한 경우에만 필요한 범위로 수행한다.

## 기술 스택

- Java 21
- Spring Boot 4.x
- Gradle 9.x
- Spring Data JPA
- QueryDSL
- Micrometer Prometheus
- PostgreSQL 17-alpine
- Docker Compose
- Prometheus, Grafana, postgres-exporter
- k6
- Testcontainers
- JUnit, AssertJ

## 프로젝트 구조 기준

- 애플리케이션 루트는 `ecommerce/`이다.
- Java 기본 패키지는 `com.dblab.ecommerce`이다.
- 인프라 파일은 프로젝트 루트의 `docker-compose.yml`과 `docker/` 하위에 있다.
- 부하 테스트 스크립트는 `k6/` 하위에 둔다.
- Phase 문서는 `docs/` 하위에 둔다.

## 구현 기준

- Phase 1까지는 나이브함을 유지한다.
- Phase 1에서는 엔티티가 FK를 `Long` 필드로 보유하는 현재 구조를 유지하고 JPA 연관관계 매핑을 추가하지 않는다.
- Phase 1에서는 `@BatchSize`, `@EntityGraph`, `JOIN FETCH`, DB 인덱스, DTO Projection을 적용하지 않는다.
- Phase 1의 N+1은 서비스 레이어 루프에서 Repository를 반복 호출해 의도적으로 재현한다.

## 검증 기준

- 구현 변경 후 필요한 범위의 Gradle 테스트를 실행한다.
- Phase 1 성능 측정 전에는 `show-sql=false`, Hikari maximum pool size 10, `VACUUM ANALYZE`를 확인한다.
- k6 부하 조건은 Phase 비교가 가능하도록 VU 50, 총 5분, 30초 Ramp-up, 요청 timeout 5초를 유지한다.
- 성능 개선은 p95, TPS, 에러율, DB 실행시간, 실행계획 등 정량 지표로 기록한다.
