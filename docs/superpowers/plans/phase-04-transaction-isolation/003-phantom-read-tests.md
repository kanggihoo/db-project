# 003 Phantom Read Tests

## Goal

Add Phantom Read tests showing that `READ COMMITTED` observes an inserted matching row while PostgreSQL `REPEATABLE READ` keeps the original snapshot.

## Files

- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java`

## Steps

- [ ] **Step 1: Add Phantom Read tests**

Add these test methods inside `TransactionIsolationTest`:

```java
    @Test
    @DisplayName("READ COMMITTED allows Phantom Read")
    void readCommittedAllowsPhantomRead() throws SQLException {
        try (Connection txA = openTransaction(Connection.TRANSACTION_READ_COMMITTED);
             Connection txB = openTransaction(Connection.TRANSACTION_READ_COMMITTED)) {

            int firstCount = countOnSaleProducts(txA, CATEGORY_ID);

            insertPhantomProduct(txB);
            txB.commit();

            int secondCount = countOnSaleProducts(txA, CATEGORY_ID);
            txA.commit();

            assertThat(firstCount).isEqualTo(1);
            assertThat(secondCount).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("PostgreSQL REPEATABLE READ prevents Phantom Read")
    void repeatableReadPreventsPhantomReadInPostgresql() throws SQLException {
        try (Connection txA = openTransaction(Connection.TRANSACTION_REPEATABLE_READ);
             Connection txB = openTransaction(Connection.TRANSACTION_READ_COMMITTED)) {

            int firstCount = countOnSaleProducts(txA, CATEGORY_ID);

            insertPhantomProduct(txB);
            txB.commit();

            int secondCount = countOnSaleProducts(txA, CATEGORY_ID);
            txA.commit();

            assertThat(firstCount).isEqualTo(1);
            assertThat(secondCount).isEqualTo(1);
        }
    }
```

Add this helper method inside the class:

```java
    private void insertPhantomProduct(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO product (id, category_id, name, description, base_price, status, is_deleted, created_at, updated_at)
                VALUES (?, ?, 'Phase4 Phantom Product', 'Phase4 phantom fixture', 20000, 'ON_SALE', false, NOW(), NOW())
                """)) {
            statement.setLong(1, PHANTOM_PRODUCT_ID);
            statement.setLong(2, CATEGORY_ID);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }
```

- [ ] **Step 2: Run focused Phantom Read tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest.readCommittedAllowsPhantomRead" --tests "*TransactionIsolationTest.repeatableReadPreventsPhantomReadInPostgresql"
```

Expected: Gradle exits 0 and both tests pass.

- [ ] **Step 3: Run all Phase 4 tests so far**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest"
```

Expected: Gradle exits 0.

- [ ] **Step 4: Commit**

```bash
git add ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java
git commit -m "test: add phase 4 phantom read tests"
```
