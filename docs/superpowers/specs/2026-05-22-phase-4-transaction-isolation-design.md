# Phase 4 Transaction Isolation Design

> PostgreSQL 격리 수준별 데이터 가시성과 동시 갱신 충돌을 Testcontainers 기반 integration test로 재현하고 증거로 남기기 위한 설계다.

## Goal

Phase 4는 PostgreSQL에서 격리 수준이 트랜잭션의 읽기 결과와 같은 row 동시 갱신 충돌에 어떤 차이를 만드는지 확인한다. 구현 테스트는 `READ COMMITTED`와 `REPEATABLE READ` 비교를 중심으로 하고, `SERIALIZABLE`은 더 강한 격리 수준과 retry 비용을 설명하는 문서상 비교 대상으로 남긴다.

핵심 학습 대상은 Dirty Read, Non-Repeatable Read, Phantom Read, Lost Update다. 이 Phase는 동시성 제어 전략을 고르는 단계가 아니라, PostgreSQL 격리 수준이 어떤 현상을 허용하거나 실패로 막는지 재현하는 단계다.

이 spec은 [Phase 4 roadmap](../../roadmap/05-phase-4-transaction-isolation.md), [Phase 4 scope](../../phases/04-transaction-isolation/scope.md), [CONTEXT.md](../../../CONTEXT.md), [ADR 0002](../../adr/0002-require-phase-evidence-under-docs-evidence.md), [ADR 0003](../../adr/0003-use-postgresql-as-the-reference-rdbms.md)를 따른다.

## Non-Goals

- 비관적 락, 낙관적 락, Atomic UPDATE, retry, idempotency 전략을 비교하지 않는다.
- `SERIALIZABLE + retry` 실무 전략을 테스트 구현 범위에 포함하지 않는다.
- Product SKU 재고 차감을 실무 정답 구현으로 완성하지 않는다.
- 주문 생성 전체 workflow, 결제, 배송, 쿠폰 정합성을 다루지 않는다.
- k6/Grafana 부하 테스트를 필수 evidence로 만들지 않는다.
- 기존 docker compose PostgreSQL volume이나 대량 seed 데이터에 의존하지 않는다.
- H2, mock DB, embedded DB로 격리 수준을 검증하지 않는다.

## Decisions

### Use Testcontainers PostgreSQL

Phase 4 테스트는 기존 docker compose PostgreSQL이 아니라 Testcontainers PostgreSQL을 사용한다.

```text
JUnit test 실행
-> PostgreSQL Testcontainer 시작
-> src/test/resources/init.sql 실행
-> 실제 schema 생성
-> Phase 4 최소 fixture reset
-> 격리 수준별 테스트 실행
-> 테스트 종료 후 container 제거
```

이 방식은 개발 DB volume 상태, 대량 seed 데이터, 로컬 실행 이력에 의존하지 않는다. PostgreSQL의 실제 MVCC와 격리 수준 동작을 확인해야 하므로 PostgreSQL Testcontainer를 사용한다.

### Use DataJpaTest Only As A DataSource Slice

테스트 클래스는 기존 테스트 패턴을 따라 `@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)`, `@Import(TestcontainersConfiguration.class)`를 사용한다.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TransactionIsolationTest {
    @Autowired DataSource dataSource;
}
```

JPA repository나 service를 검증하지 않는다. Spring test slice는 Testcontainers 설정과 `DataSource`를 얻기 위해서만 사용한다. 실제 실험은 JDBC `Connection` 두 개를 직접 열어 isolation level, commit, rollback, 실행 순서를 제어한다.

### Reset Minimal Fixture Before Each Test

각 테스트는 `@BeforeEach`에서 Phase 4 전용 최소 fixture를 known state로 되돌린다.

```text
category
- id = 900001

product
- id = 900001
- category_id = 900001
- base_price = 10000
- status = 'ON_SALE'
- is_deleted = false

product_sku
- id = 900001
- product_id = 900001
- sku_code = 'PHASE4-SKU'
- stock_quantity = 10
- extra_price = 0
```

Phantom Read 테스트가 삽입하는 `product.id = 900002`는 다음 테스트 시작 전 `@BeforeEach`에서 삭제한다. 테스트 종료 후 자동 rollback에 의존하지 않는다. 직접 획득한 JDBC connection에서 `commit()`한 변경은 남을 수 있으므로, 다음 테스트 시작 전에 항상 reset한다.

### Set Isolation Level Per Connection

격리 수준은 DB/container를 다시 만들면서 바꾸지 않는다. 각 테스트가 새 connection을 획득하고, 트랜잭션 시작 전에 isolation level을 설정한다.

```java
connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
connection.setAutoCommit(false);
```

각 테스트는 connection A/B를 열고 정해진 순서로 `SELECT`, `UPDATE`, `INSERT`, `COMMIT`, `ROLLBACK`을 수행한다.

### Treat Evidence As Test Output Plus Human Summary

테스트가 문서 파일을 자동 생성하지 않는다. 테스트는 재현과 assert에 집중하고, 실행 결과는 사람이 `docs/evidence/phase-04/`와 `docs/phases/04-transaction-isolation/report.md`에 요약한다.

k6, Grafana, `pg_stat_statements`는 선택 evidence다. Phase 4의 primary evidence는 integration test output이다.

## Requirements

### Test Class

Implementation must add one focused test class under the ecommerce test source tree.

Expected class:

```text
ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java
```

The class must:

1. Import `TestcontainersConfiguration`.
2. Inject `DataSource`.
3. Reset Phase 4 fixture rows in `@BeforeEach`.
4. Use JDBC `Connection` directly for all isolation experiments.
5. Close every connection with try-with-resources.
6. Commit or rollback every transaction explicitly.
7. Avoid service, controller, and repository assertions.

### Fixture Reset

`@BeforeEach` must perform these actions in one committed transaction:

1. Delete `product.id = 900002` if present.
2. Upsert `category.id = 900001`.
3. Upsert `product.id = 900001` with `base_price = 10000`, `status = 'ON_SALE'`, `is_deleted = false`.
4. Upsert `product_sku.id = 900001` with `stock_quantity = 10`, `extra_price = 0`.

The reset helper must be idempotent so tests can be rerun after failures.

### Dirty Read Test

Test name:

```text
dirtyReadIsPreventedEvenWithReadUncommitted
```

Required sequence:

```text
txA: READ COMMITTED or default transaction starts
txA: update product 900001 base_price to 15000 without commit
txB: READ_UNCOMMITTED transaction starts
txB: select product 900001 base_price
txA: rollback
txB: commit or rollback
```

Expected assertion:

```text
txB sees 10000, not 15000
```

This confirms PostgreSQL does not expose uncommitted changes even when `READ_UNCOMMITTED` is requested.

### Non-Repeatable Read Tests

Required tests:

```text
readCommittedAllowsNonRepeatableRead
repeatableReadPreventsNonRepeatableRead
```

READ COMMITTED sequence:

```text
txA: READ COMMITTED starts
txA: select base_price -> 10000
txB: update base_price to 15000 and commit
txA: select base_price again -> 15000
txA: commit
```

REPEATABLE READ sequence:

```text
txA: REPEATABLE READ starts
txA: select base_price -> 10000
txB: update base_price to 15000 and commit
txA: select base_price again -> 10000
txA: commit
```

The tests model live Product price reads before an Order Item Snapshot is created. They do not implement order creation.

### Phantom Read Tests

Required tests:

```text
readCommittedAllowsPhantomRead
repeatableReadPreventsPhantomReadInPostgresql
```

READ COMMITTED sequence:

```text
txA: READ COMMITTED starts
txA: count ON_SALE products in category 900001 -> 1
txB: insert product 900002 in category 900001 with status ON_SALE and commit
txA: count again -> 2
txA: commit
```

REPEATABLE READ sequence:

```text
txA: REPEATABLE READ starts
txA: count ON_SALE products in category 900001 -> 1
txB: insert product 900002 in category 900001 with status ON_SALE and commit
txA: count again -> 1
txA: commit
```

This verifies PostgreSQL's `REPEATABLE READ` snapshot behavior, including Phantom Read prevention.

### Lost Update Tests

Required tests:

```text
readCommittedCanLoseUpdateWithNaiveReadModifyWrite
repeatableReadPreventsLostUpdateWithConcurrentUpdateFailure
```

READ COMMITTED sequence:

```text
txA: READ COMMITTED starts
txB: READ COMMITTED starts
txA: select stock_quantity -> 10
txB: select stock_quantity -> 10
txA: update product_sku stock_quantity to 9 and commit
txB: update product_sku stock_quantity to 9 and commit
final select stock_quantity -> 9
```

The final stock of 9 after two stale read-modify-write attempts demonstrates Lost Update risk. This is deliberately not the Atomic UPDATE strategy.

REPEATABLE READ sequence:

```text
txA: REPEATABLE READ starts
txB: REPEATABLE READ starts
txA: select stock_quantity -> 10
txB: select stock_quantity -> 10
txA: update product_sku stock_quantity to 9 and commit
txB: update product_sku stock_quantity to 9
```

Expected assertion:

```text
txB update or commit fails with PostgreSQL concurrent update / serialization error
final stock_quantity remains 9
```

The test should assert the failure by SQL state or exception type robustly enough to avoid brittle message-only checks. Error message text can be printed as diagnostic output but should not be the only assertion.

## Evidence And Reporting

After implementation and execution, evidence should be summarized under:

```text
docs/evidence/phase-04/
├── dirty-read/
├── non-repeatable-read/
├── phantom-read/
└── lost-update/
```

Expected human-authored evidence files:

- `integration-test-output.txt`
- Optional `notes.md` for scenario interpretation

The Phase 4 report should update the isolation comparison table with:

- Dirty Read: prevented in PostgreSQL
- Non-Repeatable Read: occurs in READ COMMITTED, prevented in REPEATABLE READ
- Phantom Read: occurs in READ COMMITTED, prevented in PostgreSQL REPEATABLE READ
- Lost Update: possible with naive READ COMMITTED read-modify-write, prevented by failure in PostgreSQL REPEATABLE READ

## Acceptance Criteria

- Phase 4 tests run against PostgreSQL Testcontainers, not docker compose PostgreSQL volume.
- `src/test/resources/init.sql` creates the schema for the test container.
- Each test starts from the same minimal fixture state through `@BeforeEach`.
- The tests use direct JDBC connections and set isolation levels before transaction work begins.
- Dirty Read prevention is asserted against PostgreSQL behavior.
- Non-Repeatable Read is shown to occur under READ COMMITTED and be prevented under REPEATABLE READ.
- Phantom Read is shown to occur under READ COMMITTED and be prevented under PostgreSQL REPEATABLE READ.
- Lost Update is shown as a naive READ COMMITTED risk and as a REPEATABLE READ concurrent update failure.
- Atomic UPDATE, pessimistic lock, optimistic lock, retry, and idempotency are explicitly left for Phase 11.
- k6 and Grafana are not required to complete Phase 4.

## Resolved Questions

- Primary evidence is integration test output, not manual SQL transcript.
- Testcontainers PostgreSQL is used instead of the existing docker compose volume.
- Data volume is minimal; large seed data is not required.
- Fixture reset happens in `@BeforeEach`, not by relying on test rollback.
- Lost Update tests compare READ COMMITTED and REPEATABLE READ only.
- The test level is JDBC/DataSource based, even though the class uses a Spring test slice to obtain the configured DataSource.
