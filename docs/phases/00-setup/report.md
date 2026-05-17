# Phase 0 결과 보고서

## 결론

Phase 0은 이후 Learning Phase를 실행하기 위한 기반 역할을 충족한다.

- Docker Compose 기반 DB와 관측 인프라를 구성했다.
- PostgreSQL DDL과 JPA 엔티티를 명시적으로 맞췄다.
- JDBC Batch 기반 시딩 파이프라인을 구성했다.
- Testcontainers 기반 통합 테스트로 시딩 핵심 경로를 검증했다.
- Phase 1 Baseline을 위해 인덱스 최적화는 의도적으로 남겨두었다.

## 구현 결과

| 영역 | 결과 |
|---|---|
| 인프라 | PostgreSQL 17, Prometheus, Grafana, postgres-exporter 구성 |
| DB | Layer 0~6 테이블, FK, CHECK 제약, `pg_stat_statements` 준비 |
| Spring 설정 | JPA validate, Actuator/Prometheus, seeder profile 구성 |
| Entity | DDL 테이블과 대응되는 JPA 엔티티 구성 |
| Seeder | `BulkInsertRepository`, `DataSeeder` 기반 대량 삽입 구성 |
| 테스트 | Testcontainers와 시딩 통합 테스트 구성 |

## 검증 결과

| 검증 | 결과 |
|---|---|
| 시퀀스 ID 사전 확보 | 통과 |
| 사용자 bulk insert | 통과 |
| `order_item` FK 정합성 | 통과 |
| 사용자 등급 분포 | 통과 |

## 남은 주의사항

- `loadtest` 규모 시딩은 로컬 메모리와 디스크 상황에 따라 시간이 오래 걸릴 수 있다.
- 운영 Docker Compose와 Testcontainers의 PostgreSQL extension 조건은 다르다.
- 대량 삽입 시 JVM heap 설정을 조정해야 할 수 있다.
- Grafana dashboard의 구체적인 Phase Evidence 캡처는 Phase 1부터 본격적으로 사용한다.

## 다음 Phase로 넘기는 결정

| 결정 | 이유 |
|---|---|
| FK 인덱스를 아직 추가하지 않는다 | Phase 1 Baseline과 Phase 2 Index 개선 비교를 위해 |
| `ddl-auto=validate`를 유지한다 | DB 스키마를 코드 생성 결과가 아니라 명시적 DDL로 관리하기 위해 |
| 시딩은 JPA가 아니라 JDBC Batch를 사용한다 | 대량 데이터 준비 시간이 Phase 실험을 방해하지 않게 하기 위해 |
| 관측 도구는 Phase 0부터 켜 둔다 | 이후 Phase Evidence의 조건을 일관되게 유지하기 위해 |
