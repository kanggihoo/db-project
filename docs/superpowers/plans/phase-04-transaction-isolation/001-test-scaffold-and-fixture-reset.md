# 001 Test Scaffold And Fixture Reset

## Goal

Create the Phase 4 isolation test class, connect it to the existing PostgreSQL Testcontainers configuration, and add an idempotent `@BeforeEach` fixture reset.

## Files

- Create: `ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java`

## Steps

- [ ] **Step 1: Create the test package directory**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'ecommerce/src/test/java/com/dblab/ecommerce/isolation' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 2: Add the initial `TransactionIsolationTest` class with fixture reset**

Create `ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java` with this content:

```java
package com.dblab.ecommerce.isolation;

import com.dblab.ecommerce.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TransactionIsolationTest {

    private static final long CATEGORY_ID = 900001L;
    private static final long PRODUCT_ID = 900001L;
    private static final long PHANTOM_PRODUCT_ID = 900002L;
    private static final long SKU_ID = 900001L;
    private static final int INITIAL_PRICE = 10_000;
    private static final int UPDATED_PRICE = 15_000;
    private static final int INITIAL_STOCK = 10;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetFixture() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            deletePhantomProduct(connection);
            upsertCategory(connection);
            upsertProduct(connection);
            upsertProductSku(connection);
            connection.commit();
        }
    }

    @Test
    @DisplayName("Phase 4 fixture starts from the expected known state")
    void fixtureStartsFromKnownState() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(selectBasePrice(connection, PRODUCT_ID)).isEqualTo(INITIAL_PRICE);
            assertThat(selectStockQuantity(connection, SKU_ID)).isEqualTo(INITIAL_STOCK);
            assertThat(countOnSaleProducts(connection, CATEGORY_ID)).isEqualTo(1);
        }
    }

    private void deletePhantomProduct(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM product WHERE id = ?")) {
            statement.setLong(1, PHANTOM_PRODUCT_ID);
            statement.executeUpdate();
        }
    }

    private void upsertCategory(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO category (id, parent_id, name, depth)
                VALUES (?, NULL, 'Phase4 Category', 0)
                ON CONFLICT (id) DO UPDATE
                SET parent_id = EXCLUDED.parent_id,
                    name = EXCLUDED.name,
                    depth = EXCLUDED.depth
                """)) {
            statement.setLong(1, CATEGORY_ID);
            statement.executeUpdate();
        }
    }

    private void upsertProduct(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO product (id, category_id, name, description, base_price, status, is_deleted, created_at, updated_at)
                VALUES (?, ?, 'Phase4 Product', 'Phase4 isolation fixture', ?, 'ON_SALE', false, NOW(), NOW())
                ON CONFLICT (id) DO UPDATE
                SET category_id = EXCLUDED.category_id,
                    name = EXCLUDED.name,
                    description = EXCLUDED.description,
                    base_price = EXCLUDED.base_price,
                    status = EXCLUDED.status,
                    is_deleted = EXCLUDED.is_deleted,
                    updated_at = NOW()
                """)) {
            statement.setLong(1, PRODUCT_ID);
            statement.setLong(2, CATEGORY_ID);
            statement.setInt(3, INITIAL_PRICE);
            statement.executeUpdate();
        }
    }

    private void upsertProductSku(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO product_sku (id, product_id, sku_code, stock_quantity, extra_price)
                VALUES (?, ?, 'PHASE4-SKU', ?, 0)
                ON CONFLICT (id) DO UPDATE
                SET product_id = EXCLUDED.product_id,
                    sku_code = EXCLUDED.sku_code,
                    stock_quantity = EXCLUDED.stock_quantity,
                    extra_price = EXCLUDED.extra_price
                """)) {
            statement.setLong(1, SKU_ID);
            statement.setLong(2, PRODUCT_ID);
            statement.setInt(3, INITIAL_STOCK);
            statement.executeUpdate();
        }
    }

    private int selectBasePrice(Connection connection, long productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT base_price FROM product WHERE id = ?")) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getInt("base_price");
            }
        }
    }

    private int selectStockQuantity(Connection connection, long skuId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT stock_quantity FROM product_sku WHERE id = ?")) {
            statement.setLong(1, skuId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getInt("stock_quantity");
            }
        }
    }

    private int countOnSaleProducts(Connection connection, long categoryId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM product
                WHERE category_id = ?
                  AND status = 'ON_SALE'
                  AND is_deleted = false
                """)) {
            statement.setLong(1, categoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getInt(1);
            }
        }
    }
}
```

- [ ] **Step 3: Run the focused scaffold test**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest.fixtureStartsFromKnownState"
```

Expected: Gradle exits 0 and the single fixture test passes.

- [ ] **Step 4: Commit**

```bash
git add ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java
git commit -m "test: scaffold phase 4 isolation fixture"
```
