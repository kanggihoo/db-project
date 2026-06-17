# 005 Evidence And Report Update

## Goal

Record Phase 4 test outcomes as human-authored evidence and update the Phase 4 report with the measured isolation behavior.

## Files

- Create: `docs/evidence/phase-04/dirty-read/integration-test-output.txt`
- Create: `docs/evidence/phase-04/non-repeatable-read/integration-test-output.txt`
- Create: `docs/evidence/phase-04/phantom-read/integration-test-output.txt`
- Create: `docs/evidence/phase-04/lost-update/integration-test-output.txt`
- Modify: `docs/phases/04-transaction-isolation/report.md`

## Steps

- [ ] **Step 1: Run the complete Phase 4 test class**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest"
```

Expected: Gradle exits 0. Capture the final Gradle test summary and the list of executed `TransactionIsolationTest` methods for the evidence files.

- [ ] **Step 2: Create evidence directories**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'docs/evidence/phase-04/dirty-read','docs/evidence/phase-04/non-repeatable-read','docs/evidence/phase-04/phantom-read','docs/evidence/phase-04/lost-update' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 3: Write Dirty Read evidence**

Create `docs/evidence/phase-04/dirty-read/integration-test-output.txt` with this content:

```text
Scenario: Dirty Read prevention
Test: TransactionIsolationTest.dirtyReadIsPreventedEvenWithReadUncommitted
Database: PostgreSQL Testcontainers using src/test/resources/init.sql
Fixture: product id=900001 base_price=10000

Sequence:
1. txA updates product 900001 base_price to 15000 without commit.
2. txB requests READ_UNCOMMITTED and reads product 900001 base_price.
3. txB sees 10000, not txA's uncommitted 15000.
4. txA rolls back.

Expected: PostgreSQL prevents Dirty Read.
Result: PASS - TransactionIsolationTest completed successfully in Gradle.
```

- [ ] **Step 4: Write Non-Repeatable Read evidence**

Create `docs/evidence/phase-04/non-repeatable-read/integration-test-output.txt` with this structure:

```text
Scenario: Non-Repeatable Read
Tests:
- TransactionIsolationTest.readCommittedAllowsNonRepeatableRead
- TransactionIsolationTest.repeatableReadPreventsNonRepeatableRead
Database: PostgreSQL Testcontainers using src/test/resources/init.sql
Fixture: product id=900001 base_price=10000

READ COMMITTED:
1. txA reads base_price=10000.
2. txB updates base_price=15000 and commits.
3. txA reads base_price=15000.
Expected: Non-Repeatable Read occurs.

REPEATABLE READ:
1. txA reads base_price=10000.
2. txB updates base_price=15000 and commits.
3. txA reads base_price=10000 from its snapshot.
Expected: Non-Repeatable Read is prevented.

Result: PASS - TransactionIsolationTest completed successfully in Gradle.
```

- [ ] **Step 5: Write Phantom Read evidence**

Create `docs/evidence/phase-04/phantom-read/integration-test-output.txt` with this structure:

```text
Scenario: Phantom Read
Tests:
- TransactionIsolationTest.readCommittedAllowsPhantomRead
- TransactionIsolationTest.repeatableReadPreventsPhantomReadInPostgresql
Database: PostgreSQL Testcontainers using src/test/resources/init.sql
Fixture: category id=900001, product id=900001 status=ON_SALE

READ COMMITTED:
1. txA counts ON_SALE products in category 900001: 1.
2. txB inserts product 900002 with status=ON_SALE and commits.
3. txA counts again: 2.
Expected: Phantom Read occurs.

REPEATABLE READ:
1. txA counts ON_SALE products in category 900001: 1.
2. txB inserts product 900002 with status=ON_SALE and commits.
3. txA counts again: 1 from its snapshot.
Expected: PostgreSQL REPEATABLE READ prevents Phantom Read.

Result: PASS - TransactionIsolationTest completed successfully in Gradle.
```

- [ ] **Step 6: Write Lost Update evidence**

Create `docs/evidence/phase-04/lost-update/integration-test-output.txt` with this structure:

```text
Scenario: Lost Update
Tests:
- TransactionIsolationTest.readCommittedCanLoseUpdateWithNaiveReadModifyWrite
- TransactionIsolationTest.repeatableReadPreventsLostUpdateWithConcurrentUpdateFailure
Database: PostgreSQL Testcontainers using src/test/resources/init.sql
Fixture: product_sku id=900001 stock_quantity=10

READ COMMITTED:
1. txA reads stock_quantity=10.
2. txB reads stock_quantity=10.
3. txA writes stock_quantity=9 and commits.
4. txB writes stale stock_quantity=9 and commits.
5. Final stock_quantity is 9.
Expected: Naive read-modify-write can lose one update.

REPEATABLE READ:
1. txA reads stock_quantity=10.
2. txB reads stock_quantity=10.
3. txA writes stock_quantity=9 and commits.
4. txB attempts stale write.
5. PostgreSQL fails txB with SQL state 40001.
6. Final stock_quantity is 9.
Expected: Lost Update is prevented by concurrent update failure.

Result: PASS - TransactionIsolationTest completed successfully in Gradle.
```

- [ ] **Step 7: Update Phase 4 report**

Replace the comparison table in `docs/phases/04-transaction-isolation/report.md` with:

```markdown
| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Lost Update / Concurrent Update | Notes |
|---|---|---|---|---|---|
| READ COMMITTED | 방지 | 발생 | 발생 | naive read-modify-write에서 발생 가능 | PostgreSQL 기본 격리 수준 |
| REPEATABLE READ | 방지 | 방지 | 방지 | SQLSTATE `40001` concurrent update failure로 방지 | PostgreSQL MVCC snapshot 확인 |
| SERIALIZABLE | 방지 | 방지 | 방지 | serialization failure로 방지 | 이번 구현 테스트에서는 문서상 비교 대상으로 유지 |
```

Update the conclusion section to:

```markdown
## 결론

Phase 4는 Testcontainers PostgreSQL과 JDBC connection 두 개를 직접 제어하는 integration test로 격리 수준별 가시성 차이를 재현했다.

PostgreSQL에서는 `READ UNCOMMITTED`를 요청해도 Dirty Read가 발생하지 않았다. `READ COMMITTED`에서는 Non-Repeatable Read와 Phantom Read가 재현됐고, `REPEATABLE READ`에서는 트랜잭션 시작 시점 snapshot이 유지되어 두 현상이 방지됐다.

Lost Update는 naive read-modify-write 패턴에서 `READ COMMITTED`로 재현됐다. 같은 패턴을 `REPEATABLE READ`에서 실행하면 PostgreSQL이 SQLSTATE `40001` concurrent update failure로 stale write를 실패시켜 Lost Update가 조용히 발생하지 않았다.
```

- [ ] **Step 8: Verify evidence and report**

Run:

```bash
rtk rg -n "PASS|Dirty Read|Non-Repeatable Read|Phantom Read|Lost Update|40001" docs/evidence/phase-04 docs/phases/04-transaction-isolation/report.md
```

Expected: output includes all four scenario names and SQL state `40001`.

- [ ] **Step 9: Commit**

```bash
git add docs/evidence/phase-04 docs/phases/04-transaction-isolation/report.md
git commit -m "docs: record phase 4 isolation evidence"
```
