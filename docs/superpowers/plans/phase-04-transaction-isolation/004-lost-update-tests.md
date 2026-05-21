# 004 Lost Update Tests

## Goal

Add Lost Update tests showing the naive stale read-modify-write risk under `READ COMMITTED` and PostgreSQL's concurrent update failure under `REPEATABLE READ`.

## Files

- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java`

## Steps

- [ ] **Step 1: Add Lost Update tests**

Add these imports to `TransactionIsolationTest`:

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

Add these test methods inside the class:

```java
    @Test
    @DisplayName("READ COMMITTED can lose update with naive read-modify-write")
    void readCommittedCanLoseUpdateWithNaiveReadModifyWrite() throws SQLException {
        try (Connection txA = openTransaction(Connection.TRANSACTION_READ_COMMITTED);
             Connection txB = openTransaction(Connection.TRANSACTION_READ_COMMITTED)) {

            int stockReadByA = selectStockQuantity(txA, SKU_ID);
            int stockReadByB = selectStockQuantity(txB, SKU_ID);

            updateStockQuantity(txA, SKU_ID, stockReadByA - 1);
            txA.commit();

            updateStockQuantity(txB, SKU_ID, stockReadByB - 1);
            txB.commit();
        }

        try (Connection connection = dataSource.getConnection()) {
            assertThat(selectStockQuantity(connection, SKU_ID)).isEqualTo(9);
        }
    }

    @Test
    @DisplayName("PostgreSQL REPEATABLE READ prevents Lost Update with concurrent update failure")
    void repeatableReadPreventsLostUpdateWithConcurrentUpdateFailure() throws SQLException {
        try (Connection txA = openTransaction(Connection.TRANSACTION_REPEATABLE_READ);
             Connection txB = openTransaction(Connection.TRANSACTION_REPEATABLE_READ)) {

            int stockReadByA = selectStockQuantity(txA, SKU_ID);
            int stockReadByB = selectStockQuantity(txB, SKU_ID);

            updateStockQuantity(txA, SKU_ID, stockReadByA - 1);
            txA.commit();

            assertThatThrownBy(() -> updateStockQuantity(txB, SKU_ID, stockReadByB - 1))
                    .isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("40001"));

            txB.rollback();
        }

        try (Connection connection = dataSource.getConnection()) {
            assertThat(selectStockQuantity(connection, SKU_ID)).isEqualTo(9);
        }
    }
```

Add this helper method inside the class:

```java
    private void updateStockQuantity(Connection connection, long skuId, int stockQuantity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE product_sku SET stock_quantity = ? WHERE id = ?")) {
            statement.setInt(1, stockQuantity);
            statement.setLong(2, skuId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }
```

- [ ] **Step 2: Run focused Lost Update tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest.readCommittedCanLoseUpdateWithNaiveReadModifyWrite" --tests "*TransactionIsolationTest.repeatableReadPreventsLostUpdateWithConcurrentUpdateFailure"
```

Expected: Gradle exits 0 and both tests pass. The REPEATABLE READ test should assert SQL state `40001`.

- [ ] **Step 3: Run all Phase 4 tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest"
```

Expected: Gradle exits 0.

- [ ] **Step 4: Commit**

```bash
git add ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java
git commit -m "test: add phase 4 lost update tests"
```
