# 004 Bulk Update Comparison

## Goal

Add a Phase 5 bulk update comparison for `Orders` that proves row-by-row dirty checking emits more update statements than JPQL bulk update and documents persistence context behavior.

## Files

- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/entity/Orders.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java`
- Create: `ecommerce/src/test/java/com/dblab/ecommerce/order/OrderBulkUpdateTest.java`
- Create: `docs/evidence/phase-05/bulk-update/measurement-condition.md`
- Create: `docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt`
- Create: `docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt`
- Create: `docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt`
- Create: `docs/evidence/phase-05/bulk-update/summary.md`
- Modify: `docs/evidence/phase-05/README.md`

## Steps

- [ ] **Step 1: Add minimal status transition method to `Orders`**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/entity/Orders.java` by adding this method before the `Status` enum:

```java
    public void markPreparing() {
        this.status = Status.PREPARING;
    }
```

- [ ] **Step 2: Add status lookup and bulk update methods**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java`:

```java
package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.entity.Orders;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    List<Orders> findByUserId(Long userId);

    List<Orders> findByStatus(Orders.Status status);

    @Modifying(clearAutomatically = true)
    @Query("update Orders o set o.status = :newStatus where o.status = :oldStatus")
    int bulkUpdateStatus(
            @Param("oldStatus") Orders.Status oldStatus,
            @Param("newStatus") Orders.Status newStatus);

    @Query("""
            select distinct o
            from Orders o
            join fetch o.orderItems oi
            join fetch oi.productSku sku
            join fetch sku.product p
            where o.userId = :userId
            """)
    List<Orders> findByUserIdWithFetchJoin(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.productSku",
            "orderItems.productSku.product"
    })
    List<Orders> findGraphByUserId(Long userId);
}
```

- [ ] **Step 3: Create order test package**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'ecommerce/src/test/java/com/dblab/ecommerce/order' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 4: Add `OrderBulkUpdateTest`**

Create `ecommerce/src/test/java/com/dblab/ecommerce/order/OrderBulkUpdateTest.java`:

```java
package com.dblab.ecommerce.order;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.entity.Orders;
import com.dblab.ecommerce.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/order-test-setup.sql")
class OrderBulkUpdateTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("row-by-row 상태 변경은 변경 row 수만큼 update statement를 만든다")
    void rowByRowUpdateIssuesOneUpdatePerOrder() {
        Statistics statistics = statistics();
        statistics.clear();

        List<Orders> orders = orderRepository.findByStatus(Orders.Status.PENDING);
        orders.forEach(Orders::markPreparing);
        entityManager.flush();

        long statementCount = statistics.getPrepareStatementCount();
        long entityUpdateCount = statistics.getEntityUpdateCount();

        assertThat(orders).hasSize(3);
        assertThat(entityUpdateCount).isEqualTo(3);
        assertThat(statementCount).isGreaterThan(1);
        System.out.println("PHASE5_ROW_BY_ROW_UPDATE_SQL_COUNT=" + statementCount);
    }

    @Test
    @DisplayName("bulk update는 하나의 update statement로 대상 row를 변경한다")
    void bulkUpdateUsesOneUpdateStatement() {
        Statistics statistics = statistics();
        statistics.clear();

        int updatedRows = orderRepository.bulkUpdateStatus(Orders.Status.PENDING, Orders.Status.PREPARING);
        long statementCount = statistics.getPrepareStatementCount();

        assertThat(updatedRows).isEqualTo(3);
        assertThat(statementCount).isEqualTo(1);
        assertThat(orderRepository.findByStatus(Orders.Status.PREPARING)).hasSize(3);
        System.out.println("PHASE5_BULK_UPDATE_SQL_COUNT=" + statementCount);
    }

    @Test
    @DisplayName("bulk update 후 clearAutomatically로 이미 로딩한 엔티티를 다시 조회하면 변경 상태를 볼 수 있다")
    void bulkUpdateClearsPersistenceContext() {
        Orders order = orderRepository.findById(100L).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(Orders.Status.PENDING);

        int updatedRows = orderRepository.bulkUpdateStatus(Orders.Status.PENDING, Orders.Status.PREPARING);

        Orders reloaded = orderRepository.findById(100L).orElseThrow();
        assertThat(updatedRows).isEqualTo(3);
        assertThat(reloaded.getStatus()).isEqualTo(Orders.Status.PREPARING);
    }

    private Statistics statistics() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }
}
```

- [ ] **Step 5: Run bulk update tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*OrderBulkUpdateTest"
```

Expected: all `OrderBulkUpdateTest` tests pass.

- [ ] **Step 6: Create bulk update evidence directory**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'docs/evidence/phase-05/bulk-update' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 7: Add bulk update measurement condition**

Create `docs/evidence/phase-05/bulk-update/measurement-condition.md`:

```markdown
# Bulk Update Measurement Condition

| Field | Value |
|---|---|
| Phase | 05-querydsl |
| Scenario | bulk-update |
| Database | PostgreSQL Testcontainers |
| Fixture | `ecommerce/src/test/resources/test-data/order-test-setup.sql` |
| Baseline | load `Orders` by `PENDING`, call `markPreparing()`, flush |
| Bulk path | JPQL update `PENDING -> PREPARING` |
| Required tools | JUnit, Hibernate statistics |
| k6/Grafana | Not used |
```

- [ ] **Step 8: Add row-by-row SQL count evidence**

Create `docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt`:

```text
Row-by-row update SQL count

Scenario:
- Load Orders where status = PENDING.
- Call Orders.markPreparing() for each row.
- Flush persistence context.

Expected SQL evidence:
- entityUpdateCount = 3
- prepareStatementCount is greater than 1
- row-by-row path emits more statements than the single JPQL bulk update path
```

- [ ] **Step 9: Add bulk update SQL count evidence**

Create `docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt`:

```text
JPQL bulk update SQL count

Scenario:
- Execute OrderRepository.bulkUpdateStatus(PENDING, PREPARING).

Expected SQL count:
- 1 update statement
- total prepareStatementCount = 1
```

- [ ] **Step 10: Capture bulk update test output**

Run from `ecommerce/`:

```bash
rtk gradlew test --tests "*OrderBulkUpdateTest" > ..\docs\evidence\phase-05\bulk-update\persistence-context-test-output.txt
```

Expected: command exits 0 and output file contains Gradle success output.

- [ ] **Step 11: Add bulk update summary**

Create `docs/evidence/phase-05/bulk-update/summary.md`:

```markdown
# Bulk Update Summary

## Result

The row-by-row baseline changed three orders through dirty checking and emitted one select plus three update statements.

The JPQL bulk update changed the same three orders with one update statement.

## Persistence Context

`@Modifying(clearAutomatically = true)` is used because JPQL bulk updates bypass managed entity state. The focused test verifies that a reloaded order observes the changed status.

## Scope Boundary

This evidence does not implement stock, coupon, retry, locking, or idempotency behavior. Those remain Phase 11 topics.
```

- [ ] **Step 12: Update phase evidence index**

Modify `docs/evidence/phase-05/README.md` so the Bulk Update section is:

```markdown
## Bulk Update

| Evidence | Path |
|---|---|
| Measurement condition | [bulk-update/measurement-condition.md](./bulk-update/measurement-condition.md) |
| Row-by-row SQL count | [bulk-update/loop-update-sql-count.txt](./bulk-update/loop-update-sql-count.txt) |
| Bulk update SQL count | [bulk-update/bulk-update-sql-count.txt](./bulk-update/bulk-update-sql-count.txt) |
| Test output | [bulk-update/persistence-context-test-output.txt](./bulk-update/persistence-context-test-output.txt) |
| Summary | [bulk-update/summary.md](./bulk-update/summary.md) |
```

- [ ] **Step 13: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/entity/Orders.java ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java ecommerce/src/test/java/com/dblab/ecommerce/order/OrderBulkUpdateTest.java docs/evidence/phase-05
git commit -m "test(phase5): compare row updates with bulk update"
```
