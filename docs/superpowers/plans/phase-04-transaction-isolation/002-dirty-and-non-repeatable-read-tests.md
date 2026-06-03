# 002 Dirty And Non-Repeatable Read Tests

## Goal

Add the Dirty Read prevention test and Non-Repeatable Read comparison tests for `READ COMMITTED` and `REPEATABLE READ`.

## Files

- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java`

## Steps

- [ ] **Step 1: Add Dirty Read and Non-Repeatable Read tests**

Modify `TransactionIsolationTest` by adding these test methods inside the class:

Add these test methods inside the class:

```java
    @Test
    @DisplayName("PostgreSQL prevents Dirty Read even when READ_UNCOMMITTED is requested")
    void dirtyReadIsPreventedEvenWithReadUncommitted() throws SQLException {
        try (Connection txA = openTransaction(Connection.TRANSACTION_READ_COMMITTED);
             Connection txB = openTransaction(Connection.TRANSACTION_READ_UNCOMMITTED)) {

            updateBasePrice(txA, PRODUCT_ID, UPDATED_PRICE);

            int visiblePriceBeforeCommit = selectBasePrice(txB, PRODUCT_ID);

            txA.rollback();
            txB.rollback();

            assertThat(visiblePriceBeforeCommit).isEqualTo(INITIAL_PRICE);
        }
    }

    @Test
    @DisplayName("READ COMMITTED allows Non-Repeatable Read")
    void readCommittedAllowsNonRepeatableRead() throws SQLException {
        try (Connection txA = openTransaction(Connection.TRANSACTION_READ_COMMITTED);
             Connection txB = openTransaction(Connection.TRANSACTION_READ_COMMITTED)) {

            int firstRead = selectBasePrice(txA, PRODUCT_ID);

            updateBasePrice(txB, PRODUCT_ID, UPDATED_PRICE);
            txB.commit();

            int secondRead = selectBasePrice(txA, PRODUCT_ID);
            txA.commit();

            assertThat(firstRead).isEqualTo(INITIAL_PRICE);
            assertThat(secondRead).isEqualTo(UPDATED_PRICE);
        }
    }

    @Test
    @DisplayName("REPEATABLE READ prevents Non-Repeatable Read")
    void repeatableReadPreventsNonRepeatableRead() throws SQLException {
        try (Connection txA = openTransaction(Connection.TRANSACTION_REPEATABLE_READ);
             Connection txB = openTransaction(Connection.TRANSACTION_READ_COMMITTED)) {

            int firstRead = selectBasePrice(txA, PRODUCT_ID);

            updateBasePrice(txB, PRODUCT_ID, UPDATED_PRICE);
            txB.commit();

            int secondRead = selectBasePrice(txA, PRODUCT_ID);
            txA.commit();

            assertThat(firstRead).isEqualTo(INITIAL_PRICE);
            assertThat(secondRead).isEqualTo(INITIAL_PRICE);
        }
    }
```

Add these helper methods inside the class:

```java
    private Connection openTransaction(int isolationLevel) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setTransactionIsolation(isolationLevel);
        connection.setAutoCommit(false);
        return connection;
    }

    private void updateBasePrice(Connection connection, long productId, int basePrice) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE product SET base_price = ?, updated_at = NOW() WHERE id = ?")) {
            statement.setInt(1, basePrice);
            statement.setLong(2, productId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }
```

- [ ] **Step 2: Run focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest.dirtyReadIsPreventedEvenWithReadUncommitted" --tests "*TransactionIsolationTest.readCommittedAllowsNonRepeatableRead" --tests "*TransactionIsolationTest.repeatableReadPreventsNonRepeatableRead"
```

Expected: Gradle exits 0 and all three tests pass.

- [ ] **Step 3: Run all Phase 4 tests so far**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest"
```

Expected: Gradle exits 0.

- [ ] **Step 4: Commit**

```bash
git add ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java
git commit -m "test: add phase 4 read anomaly tests"
```
