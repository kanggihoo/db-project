# 006 EntityGraph Strategy

## Goal

Implement `strategy=entity-graph` using repository-level `@EntityGraph` and compare it with Fetch Join without duplicating service logic.

## Files

- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java`
- Modify: `docs/phases/03-n-plus-one/observability.md`

## Steps

- [ ] **Step 1: Add failing EntityGraph repository test**

In `OrderRepositoryTest`, add:

```java
@Test
void EntityGraph_조회는_OrderItem_SKU_Product_연관을_조회시점에_로딩한다() {
    Session session = entityManager.unwrap(Session.class);
    Statistics statistics = session.getSessionFactory().getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    List<Orders> orders = orderRepository.findGraphByUserId(savedUserId);
    long afterQuery = statistics.getPrepareStatementCount();

    orders.forEach(order -> order.getOrderItems().forEach(item -> {
        item.getProductSku().getProduct().getId();
    }));

    long afterTraversal = statistics.getPrepareStatementCount();
    assertThat(afterTraversal).isEqualTo(afterQuery);
}
```

Expected: compilation fails because `findGraphByUserId` does not exist.

- [ ] **Step 2: Add EntityGraph repository method**

Modify `OrderRepository.java`:

```java
import org.springframework.data.jpa.repository.EntityGraph;

@EntityGraph(attributePaths = {
        "orderItems",
        "orderItems.productSku",
        "orderItems.productSku.product"
})
List<Orders> findGraphByUserId(Long userId);
```

- [ ] **Step 3: Wire service dispatch**

Modify `OrderService`:

```java
public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategy strategy) {
    return switch (strategy) {
        case LAZY -> getOrdersByUserIdLazy(userId);
        case FETCH_JOIN -> getOrdersByUserIdFetchJoin(userId);
        case BATCH_SIZE -> getOrdersByUserIdLazy(userId);
        case ENTITY_GRAPH -> getOrdersByUserIdEntityGraph(userId);
    };
}

private List<OrderResponse> getOrdersByUserIdEntityGraph(Long userId) {
    return orderRepository.findGraphByUserId(userId)
            .stream()
            .map(OrderResponse::from)
            .toList();
}
```

- [ ] **Step 4: Update observability notes**

In `docs/phases/03-n-plus-one/observability.md`, add:

```markdown
## EntityGraph 해석

EntityGraph는 repository method에 조회 시점 로딩 범위를 선언한다. Phase 3에서는 Fetch Join과 유사하게 `OrderItems -> ProductSku -> Product` 반복 select를 줄이는지 확인하고, 실제 SQL shape는 `representative-sql.txt`와 `pg-stat-statements.txt`에 기록한다.
```

- [ ] **Step 5: Run focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests com.dblab.ecommerce.repository.OrderRepositoryTest
```

Expected: repository tests pass.

- [ ] **Step 6: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java docs/phases/03-n-plus-one/observability.md
git commit -m "feat: add phase 3 entity graph strategy"
```
