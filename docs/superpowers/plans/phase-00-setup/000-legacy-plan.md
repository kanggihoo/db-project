# SETUP.md 대용량 데이터 시딩(Seeding) 구현 계획

이 계획서는 `SETUP.md`에 명시된 복잡한 이커머스 도메인(Layer 0 ~ Layer 6)을 실제로 구축하고, Java Faker와 JdbcTemplate을 활용해 대용량 데이터를 빠르고 정합성에 맞게 삽입하기 위한 아키텍처 및 코드 구현 상세 명세입니다.

## User Review Required

> [!IMPORTANT]
> - **데이터 크기 (Data Volume)**: `users` 10만 건, `orders`, `point_history` 각 100만 건 수준을 생성하는 데 로컬 환경 사양에 따라 수 분의 시간이 소요될 수 있습니다. 최초 구현 시에는 이보다 작은 사이즈(예: 1/10 수준)로 빠르게 파이프라인을 검증한 뒤 실제 사이즈로 늘릴 것을 권장합니다. 이에 동의하시는지 확인이 필요합니다.

## Proposed Changes

---

### 인프라 및 DB 스키마 (Infrastructure)

> PostgreSQL을 띄우고 영속성 볼륨을 물리적으로 구성하며, Layer 규칙이 반영된 뼈대를 잡습니다.

#### [NEW] [docker-compose.yml](file:///Users/kkh/Desktop/db-project/docker-compose.yml)
- `postgres:17-alpine` 이미지 지정
- `postgres-data` 볼륨 매핑 적용
- 추후 모니터링을 위한 prometheus, grafana 포트 및 환경 설정 반영

#### [NEW] [init.sql](file:///Users/kkh/Desktop/db-project/docker/postgres/init.sql)
- Layer 0부터 Layer 6까지 의존성 순서대로 `CREATE TABLE` 문 작성
- FK 무결성 제약 조건 포함
- `ALTER TABLE`을 활용한 `CHECK` 제약조건 설정
- `pg_stat_statements` 확장을 설치하는 명령어 삽입

---

### Spring Boot 설정 및 의존성 (Configuration)

> 대량 데이터 처리에 필수적인 JDBC 배치 삽입 성능을 극대화하기 위한 설정을 추가합니다.

#### [MODIFY] [build.gradle](file:///Users/kkh/Desktop/db-project/build.gradle)
- `net.datafaker:datafaker` (구버전 java-faker 대신 최신 유지보수되는 DataFaker 권장)
- `spring-boot-starter-data-jpa` 및 `spring-boot-starter-data-jdbc` 추가
- `org.postgresql:postgresql` 드라이버 추가

#### [NEW] [application.yml](file:///Users/kkh/Desktop/db-project/src/main/resources/application.yml)
- `spring.jpa.hibernate.ddl-auto=validate` 로 설정하여 init.sql 의존.

#### [NEW] [application-seeder.yml](file:///Users/kkh/Desktop/db-project/src/main/resources/application-seeder.yml)
- Datasource URL에 `reWriteBatchedInserts=true` 파라미터 강제 추가 (배치 인서트 속도 폭발적 증가)
- `logging.level` 조정을 통해 과도한 쿼리 로그 출력 방지 방안 마련

---

### 벌크 데이터 리포지토리 및 ID 추출 유틸 (Repositories)

> JPA의 영속성 컨텍스트 오버헤드를 우회하여 순수 Batch Insert를 수행할 핵심 로직입니다.

#### [NEW] [IdProvider.java](file:///Users/kkh/Desktop/db-project/src/main/java/com/example/seeder/IdProvider.java)
- PostgreSQL의 `generate_series` 와 시퀀스를 호출하여 지정한 갯수만큼 **ID 배열을 사전에 반환하는 로직 (ID Pre-allocation).**

#### [NEW] [BulkInsertRepository.java](file:///Users/kkh/Desktop/db-project/src/main/java/com/example/repository/BulkInsertRepository.java)
- `JdbcTemplate.batchUpdate()` 를 이용해 테이블별로 `INSERT INTO` 구문을 실행하는 메소드 구현 `bulkInsertUsers()`, `bulkInsertOrders()` 등.
- Chunk 크기(5,000~10,000)를 계산하여 분할 전송하는 로직 내장.

---

### 데이터 시더 파이프라인 (Data Seeder Pipeline)

> Faker로 데이터를 만들어내고, IdProvider로 연결고리를 만들며 순차적으로 실행시키는 메인 컨트롤러.

#### [NEW] [DataSeeder.java](file:///Users/kkh/Desktop/db-project/src/main/java/com/example/seeder/DataSeeder.java)
- `@Profile("seeder")` 이며 `CommandLineRunner` 구현체로 앱 실행 시 구동됨.
- `isAlreadySeeded()` 를 통해 데이터 존재 여부 검사 로직(예: Users Count 조회) 구현.
- `Layer 0` -> ID 생성 및 리스트 삽입 (Users_IDs, Category_IDs 등 보관)
- `Layer 1` -> 부모 ID 랜덤 조합을 통한 자식 데이터 Mocking (Product, Address 등) 삽입
- ~ 반복 ~ 마지막 Layer 6 파이프라인까지 순차 제어.
- `DataFaker` 활용 통계치(등급, 상태 분포도) 커스텀 로직 반영.

---

## Open Questions

> [!CAUTION]
> 1. `DataFaker` 모듈에는 기본적으로 "한국인 이름", "한국 주소" 로케일 등도 지원됩니다. 무작위 영문 데이터보다 한국어 기반의 데이터로 세팅하는 것이 더 식별하기 좋을지 의견 부탁드립니다.

## Verification Plan

수현 구현 후 다음 단계들로 시스템 동작을 검증합니다.

### 1. 인프라스트럭처 검토
- `docker-compose up -d` 명령어 실행
- 터미널을 통해 `postgres-data` 볼륨이 올바르게 생성되고 `init.sql`이 1회 실행되는지 콘솔 출력 검증

### 2. 시더 프로필 실행 및 배치 관측
- 로컬 환경에서 `./gradlew bootRun --args='--spring.profiles.active=seeder'` 실행
- Batch Insert 수행 중 OOM(Out of Memory) 발생 유무 파악
- 레이어별 실행 소요시간 로깅 (목표 총 소요시간 N분 단위 책정)

### 3. PostgreSQL 정합성 및 분포 쿼리 검문
- `SETUP.md`에 명시된 전체 Layer별 ROW COUNT 점검 쿼리문 실행 (`psql` 접속 또는 DBeaver 등 도구 사용)
- `SELECT COUNT(*) FROM order_item oi LEFT JOIN orders o ON o.id = oi.order_id WHERE o.id IS NULL` 과 같은 고아객체 결함 여부 체크
- Users Grade, Product Status 데이터 분포 퍼센티지 일치 점검
