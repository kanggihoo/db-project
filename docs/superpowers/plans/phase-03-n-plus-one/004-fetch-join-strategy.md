# 004 Fetch Join Strategy

## Goal

Implement `strategy=fetch-join` with repository fetch joins and verify it reduces the lazy traversal SQL count.

## Files

- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java`

## Steps

- [ ] **Step 1: Add failing fetch-join SQL count test**

In `OrderRepositoryTest`, add:

```java
@Test
void FetchJoin_조회는_OrderItem_SKU_Product_반복조회를_줄인다() {
    Session session = entityManager.unwrap(Session.class);
    Statistics statistics = session.getSessionFactory().getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    List<Orders> orders = orderRepository.findByUserIdWithFetchJoin(savedUserId);
    long afterQuery = statistics.getPrepareStatementCount();

    orders.forEach(order -> order.getOrderItems().forEach(item -> {
        item.getProductSku().getProduct().getId();
    }));

    long afterTraversal = statistics.getPrepareStatementCount();
    assertThat(afterTraversal).isEqualTo(afterQuery);
}
```

Expected: compilation fails because `findByUserIdWithFetchJoin` does not exist.

- [ ] **Step 2: Run the focused failing test**

Run:

```bash
cd ecommerce && rtk gradlew test --tests com.dblab.ecommerce.repository.OrderRepositoryTest
```

Expected: compilation fails for missing repository method.

- [ ] **Step 3: Add fetch-join repository method**

Modify `OrderRepository.java`:

```java
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Query("""
        select distinct o
        from Orders o
        join fetch o.orderItems oi
        join fetch oi.productSku sku
        join fetch sku.product p
        where o.userId = :userId
        """)
List<Orders> findByUserIdWithFetchJoin(@Param("userId") Long userId);
```

Do not fetch join `Product.images` in this first method. Fetching two collections together can multiply rows heavily. Product images will be handled by lazy access or a later constrained strategy.

- [ ] **Step 4: Wire service dispatch**

Modify `OrderService`:

```java
public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategy strategy) {
    return switch (strategy) {
        case LAZY -> getOrdersByUserIdLazy(userId);
        case FETCH_JOIN -> getOrdersByUserIdFetchJoin(userId);
        case BATCH_SIZE, ENTITY_GRAPH -> getOrdersByUserIdLazy(userId);
    };
}

private List<OrderResponse> getOrdersByUserIdFetchJoin(Long userId) {
    return orderRepository.findByUserIdWithFetchJoin(userId)
            .stream()
            .map(OrderResponse::from)
            .toList();
}
```

- [ ] **Step 5: Run focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests com.dblab.ecommerce.repository.OrderRepositoryTest
```

Expected: repository tests pass.

- [ ] **Step 6: Record fetch-join limitation in Phase observability docs**

In `docs/phases/03-n-plus-one/observability.md`, add:

```markdown
## Fetch Join 해석

Fetch Join은 `Orders -> OrderItems -> ProductSku -> Product` 구간의 반복 select를 줄인다. `Product.images`는 두 번째 collection이므로 동시에 fetch join하면 row multiplication이 커질 수 있다. Phase 3 report는 fetch join SQL count와 row duplication 위험을 함께 기록한다.
```

- [ ] **Step 7: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java docs/phases/03-n-plus-one/observability.md
git commit -m "feat: add phase 3 fetch join strategy"
```
