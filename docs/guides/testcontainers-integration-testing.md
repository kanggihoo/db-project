# Testcontainers Integration Testing Guide

> PostgreSQL Testcontainers를 사용해 로컬 docker compose volume과 분리된 integration test DB를 구성하는 기준을 정리한다.

## When To Use

Testcontainers는 Learning Phase의 목표가 PostgreSQL 자체 동작을 반복 가능하게 확인하는 경우에 사용한다.

| Use Case | Recommended |
|---|---|
| PostgreSQL 격리 수준, MVCC, transaction failure 재현 | Yes |
| 개발 DB volume 상태와 무관한 반복 테스트 | Yes |
| 대량 k6 부하 측정, Grafana time-series evidence | No, use docker compose |
| 운영과 유사한 long-running observability run | No, use docker compose |

Phase 4 transaction isolation 테스트는 PostgreSQL MVCC와 격리 수준 동작이 핵심이므로 Testcontainers를 사용한다.

## Existing Test Configuration

공통 Testcontainers 설정은 test source에 있다.

```text
ecommerce/src/test/java/com/dblab/ecommerce/TestcontainersConfiguration.java
```

이 설정은 `postgres:17-alpine` 컨테이너를 띄우고 `src/test/resources/init.sql`로 실제 schema를 만든다.

테스트 클래스는 기존 패턴을 따른다.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TransactionIsolationTest {
    @Autowired DataSource dataSource;
}
```

`@DataJpaTest`는 여기서 JPA repository 검증 목적이 아니라 Testcontainers-backed `DataSource`를 얻기 위한 slice로 사용한다.

## Data Lifecycle

Testcontainers DB는 docker compose PostgreSQL volume을 재사용하지 않는다.

```text
test starts
-> PostgreSQL Testcontainer starts
-> init.sql creates schema
-> test inserts or resets minimal fixture
-> test assertions run
-> container is removed after test context lifecycle
```

대량 seed 데이터는 사용하지 않는다. Phase별 테스트가 필요한 최소 row만 직접 준비한다.

## Fixture Reset Pattern

직접 JDBC connection에서 `commit()`한 변경은 Spring test rollback에 의존하지 않는다. 각 테스트 시작 전에 `@BeforeEach`에서 known state로 되돌린다.

```java
@BeforeEach
void resetFixture() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
        connection.setAutoCommit(false);

        // delete temporary rows first
        // upsert stable fixture rows next

        connection.commit();
    }
}
```

Phase 4 최소 fixture:

```text
category id=900001
product id=900001
product_sku id=900001
temporary phantom product id=900002
```

## Transaction Isolation Pattern

격리 수준은 DB를 다시 만들면서 바꾸지 않는다. 각 JDBC connection에서 트랜잭션 작업 전에 설정한다.

```java
private Connection openTransaction(int isolationLevel) throws SQLException {
    Connection connection = dataSource.getConnection();
    connection.setTransactionIsolation(isolationLevel);
    connection.setAutoCommit(false);
    return connection;
}
```

테스트는 두 connection을 열고 순서를 명시한다.

```text
txA: begin with isolation level
txB: begin with isolation level
txA: SELECT
txB: UPDATE or INSERT
txB: COMMIT
txA: SELECT again
txA: COMMIT or ROLLBACK
```

모든 connection은 try-with-resources로 닫고, 모든 transaction은 직접 `commit()` 또는 `rollback()`한다.

## Running Focused Tests

프로젝트 루트에서 실행한다.

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest"
```

전체 테스트도 같은 Testcontainers 설정을 사용할 수 있다.

```bash
cd ecommerce && rtk gradlew test
```

Docker daemon은 실행 중이어야 한다. docker compose PostgreSQL은 실행 중일 필요가 없다.

## Evidence Policy

Testcontainers integration test는 evidence 파일을 자동 생성하지 않는다. 테스트는 assert에 집중하고, 실행 결과는 사람이 `docs/evidence/<phase>/`와 phase report에 요약한다.

Phase 4 primary evidence:

```text
docs/evidence/phase-04/*/integration-test-output.txt
docs/phases/04-transaction-isolation/report.md
```

k6, Grafana, `pg_stat_statements`는 Phase 4에서는 선택 evidence다.
