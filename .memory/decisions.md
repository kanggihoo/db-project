# Decisions

## Phase별 최적화 순서 유지

- 날짜: 2026-05-08
- 상태: active
- 결정: 인덱스, Fetch Join, BatchSize, EntityGraph, DTO Projection, QueryDSL 등 최적화는 RoadMap의 Phase 순서에 맞춰 단계적으로 도입한다.
- 이유: 이 프로젝트의 목적은 각 병목을 먼저 수치로 확인한 뒤 해결책을 하나씩 적용해 Before/After를 증명하는 것이다.
- 버린 대안: 초기 구현부터 실무형 최적화를 모두 적용한다.
- 재검토 조건: 프로젝트 목적이 학습용 실험에서 실제 서비스형 구현으로 바뀌는 경우

## Phase 0과 Phase 1에서는 FK 인덱스를 의도적으로 두지 않음

- 날짜: 2026-05-08
- 상태: active
- 결정: Phase 0/1 기준 스키마에는 FK 인덱스를 추가하지 않고, Phase 2에서 인덱스 추가 전후를 비교한다.
- 이유: PostgreSQL은 FK 인덱스를 자동 생성하지 않으며, 인덱스 부재로 인한 Seq Scan과 조인 병목을 Phase 1 베이스라인으로 확보해야 한다.
- 버린 대안: DDL 작성 시 FK 컬럼마다 인덱스를 미리 생성한다.
- 재검토 조건: Phase 1 베이스라인 기록이 완료되어 Phase 2 인덱스 실험으로 진입하는 경우

## 스키마는 init.sql과 JPA validate로 관리

- 날짜: 2026-05-08
- 상태: active
- 결정: PostgreSQL 스키마는 `docker/postgres/init.sql`로 명시 관리하고, Spring JPA는 `ddl-auto=validate`로 스키마 일치 여부만 검증한다.
- 이유: CHECK 제약, PostgreSQL 확장, 향후 부분 인덱스/표현식 인덱스 등 DB 고유 기능을 보존해야 한다.
- 버린 대안: Hibernate `ddl-auto=create` 또는 `update`에 스키마 생성을 맡긴다.
- 재검토 조건: DBMS를 교체하거나 SQL DDL 관리 전략을 바꾸는 경우

## 대용량 시딩은 JDBC Batch Insert와 ID 사전 할당 사용

- 날짜: 2026-05-08
- 상태: active
- 결정: 대용량 더미 데이터 삽입은 JPA `save()`가 아니라 `JdbcTemplate.batchUpdate()`와 시퀀스 ID 사전 할당으로 처리한다.
- 이유: 100만 건 수준 데이터에서 JPA 영속성 컨텍스트 오버헤드를 피하고, FK Layer 간 참조 ID를 삽입 전에 확보해야 한다.
- 버린 대안: Entity를 생성해 JPA Repository로 단건 또는 반복 저장한다.
- 재검토 조건: 시딩 규모가 작아져 구현 단순성이 성능보다 중요해지는 경우
