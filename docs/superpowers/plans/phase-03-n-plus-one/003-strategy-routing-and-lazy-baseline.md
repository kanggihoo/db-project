# 003 Strategy Routing And Lazy Baseline

## Goal

Route `GET /api/orders?userId=&strategy=` through a typed strategy value and make `strategy=lazy` reproduce association-based N+1.

## Files

- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderLoadingStrategy.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java`

## Steps

- [ ] **Step 1: Add failing strategy parsing tests**

Create `ecommerce/src/test/java/com/dblab/ecommerce/service/OrderLoadingStrategyTest.java`:

```java
package com.dblab.ecommerce.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLoadingStrategyTest {

    @Test
    void 빈_전략은_lazy로_처리한다() {
        assertThat(OrderLoadingStrategy.from(null)).isEqualTo(OrderLoadingStrategy.LAZY);
        assertThat(OrderLoadingStrategy.from("")).isEqualTo(OrderLoadingStrategy.LAZY);
    }

    @Test
    void kebab_case_전략을_enum으로_변환한다() {
        assertThat(OrderLoadingStrategy.from("fetch-join")).isEqualTo(OrderLoadingStrategy.FETCH_JOIN);
        assertThat(OrderLoadingStrategy.from("batch-size")).isEqualTo(OrderLoadingStrategy.BATCH_SIZE);
        assertThat(OrderLoadingStrategy.from("entity-graph")).isEqualTo(OrderLoadingStrategy.ENTITY_GRAPH);
    }

    @Test
    void 알수없는_전략은_예외를_던진다() {
        assertThatThrownBy(() -> OrderLoadingStrategy.from("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported order loading strategy");
    }
}
```

- [ ] **Step 2: Run strategy parsing test and verify it fails**

Run:

```bash
cd ecommerce && rtk gradlew test --tests com.dblab.ecommerce.service.OrderLoadingStrategyTest
```

Expected: compilation fails because `OrderLoadingStrategy` does not exist.

- [ ] **Step 3: Create `OrderLoadingStrategy`**

Create `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderLoadingStrategy.java`:

```java
package com.dblab.ecommerce.service;

import java.util.Arrays;

public enum OrderLoadingStrategy {
    LAZY("lazy"),
    FETCH_JOIN("fetch-join"),
    BATCH_SIZE("batch-size"),
    ENTITY_GRAPH("entity-graph");

    private final String value;

    OrderLoadingStrategy(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OrderLoadingStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return LAZY;
        }
        return Arrays.stream(values())
                .filter(strategy -> strategy.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported order loading strategy: " + value));
    }
}
```

- [ ] **Step 4: Run strategy parsing test and verify it passes**

Run:

```bash
cd ecommerce && rtk gradlew test --tests com.dblab.ecommerce.service.OrderLoadingStrategyTest
```

Expected: test passes.

- [ ] **Step 5: Route controller requests by strategy**

Modify `OrderController.getOrders`:

```java
@GetMapping
public List<OrderResponse> getOrders(
        @RequestParam Long userId,
        @RequestParam(required = false) String strategy) {
    return orderService.getOrdersByUserId(userId, OrderLoadingStrategy.from(strategy));
}
```

Add import:

```java
import com.dblab.ecommerce.service.OrderLoadingStrategy;
```

- [ ] **Step 6: Implement lazy strategy dispatch**

Modify `OrderService`:

```java
public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategy strategy) {
    return switch (strategy) {
        case LAZY -> getOrdersByUserIdLazy(userId);
        case FETCH_JOIN, BATCH_SIZE, ENTITY_GRAPH -> getOrdersByUserIdLazy(userId);
    };
}

private List<OrderResponse> getOrdersByUserIdLazy(Long userId) {
    List<Orders> orders = orderRepository.findByUserId(userId);
    return orders.stream().map(OrderResponse::from).toList();
}
```

Keep a compatibility method if any caller still uses the old signature:

```java
public List<OrderResponse> getOrdersByUserId(Long userId) {
    return getOrdersByUserId(userId, OrderLoadingStrategy.LAZY);
}
```

Change `OrderResponse.of` to a one-argument factory:

```java
public static OrderResponse from(Orders order) {
    return new OrderResponse(
            order.getId(),
            order.getUserId(),
            order.getStatus(),
            order.getFinalPrice(),
            order.getCreatedAt(),
            order.getOrderItems().stream().map(OrderItemDto::from).toList());
}
```

- [ ] **Step 7: Add SQL count assertion for lazy association traversal**

In `OrderRepositoryTest`, add:

```java
@Test
void LAZY_연관접근은_OrderItem_SKU_Product_Image_조회_SQL을_추가로_발생시킨다() {
    Session session = entityManager.unwrap(Session.class);
    Statistics statistics = session.getSessionFactory().getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    List<Orders> orders = orderRepository.findByUserId(savedUserId);
    long beforeTraversal = statistics.getPrepareStatementCount();

    orders.forEach(order -> order.getOrderItems().forEach(item -> {
        item.getProductSku().getProduct().getImages().forEach(ProductImage::getImageUrl);
    }));

    long extraSqlCount = statistics.getPrepareStatementCount() - beforeTraversal;
    assertThat(extraSqlCount).isGreaterThanOrEqualTo(4);
}
```

- [ ] **Step 8: Run focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests com.dblab.ecommerce.service.OrderLoadingStrategyTest --tests com.dblab.ecommerce.repository.OrderRepositoryTest
```

Expected: both test classes pass.

- [ ] **Step 9: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java ecommerce/src/main/java/com/dblab/ecommerce/service/OrderLoadingStrategy.java ecommerce/src/main/java/com/dblab/ecommerce/dto/OrderResponse.java ecommerce/src/test/java/com/dblab/ecommerce
git commit -m "feat: route order loading strategies"
```
