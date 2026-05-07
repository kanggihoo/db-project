# Known Issues

## Phase 1 베이스라인 증빙 미완

- 상태: open
- 내용: Phase 1 구현은 완료됐지만 부하테스트 실행, 증빙 스크린샷 4장, `BASELINE.md` 정량 기록이 아직 완료되지 않았다.
- 영향: Phase 2 인덱스 최적화로 넘어가면 Before 기준선이 부정확해진다.
- 다음 조치: `docs/PHASE1-OVERVIEW.md`의 직접 실행 항목을 완료한다.

## pg_stat_statements 테스트 환경 제약

- 상태: known
- 내용: Testcontainers 기본 PostgreSQL 이미지에서는 `shared_preload_libraries` 설정 문제로 `pg_stat_statements` 확장 생성이 제한될 수 있어 테스트용 `init.sql`에서는 제거되어 있다.
- 영향: 운영 Docker Compose와 테스트 컨테이너의 확장 구성 차이가 있다.
- 다음 조치: 운영 인프라에서는 `docker-compose up` 후 `SELECT * FROM pg_stat_statements LIMIT 1;`로 확인한다.

## 대량 시딩 시 JVM 힙 필요

- 상태: known
- 내용: 현재 문서 기준 소량 테스트 모드에서는 문제 없지만 100만 건 수준 삽입 시 JVM 힙 설정이 필요할 수 있다.
- 영향: 시더 실행 중 OOM 가능성이 있다.
- 다음 조치: 대량 삽입 시 `-Dorg.gradle.jvmargs="-Xmx2g"` 적용을 검토한다.

## Grafana 대시보드 수동 구성 필요

- 상태: open
- 내용: Prometheus 데이터소스는 프로비저닝되지만 PostgreSQL 대시보드 패널은 수동 import 또는 구성이 필요하다.
- 영향: Phase별 증빙 스크린샷 캡처 준비가 지연될 수 있다.
- 다음 조치: 필요 시 Grafana PostgreSQL Database 대시보드 import를 수행한다.
