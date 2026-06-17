# 002 Order Loading Strategy Registry

## Goal

Phase 3 Order loading 전략을 Spring strategy bean + registry 구조로 분리하고 `OrderService`의 switch를 제거한다.

## Files

- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/order/OrderLoadingStrategy.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/order/OrderLoadingStrategyRegistry.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/order/LazyOrderLoadingStrategy.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/order/FetchJoinOrderLoadingStrategy.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/order/BatchSizeOrderLoadingStrategy.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/order/EntityGraphOrderLoadingStrategy.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java`
- Create: `ecommerce/src/test/java/com/dblab/ecommerce/service/order/OrderLoadingStrategyRegistryTest.java`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/service/OrderLoadingStrategyNameTest.java`

## Steps

- [ ] **Step 1: Add registry unit test**

Create `ecommerce/src/test/java/com/dblab/ecommerce/service/order/OrderLoadingStrategyRegistryTest.java`:

```java
package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLoadingStrategyRegistryTest {

    @Test
    @DisplayName("빈 전략명은 lazy 전략으로 처리한다")
    void shouldResolveBlankNameToLazy() {
        StubOrderLoadingStrategy lazy = new StubOrderLoadingStrategy("lazy");
        OrderLoadingStrategyRegistry registry = new OrderLoadingStrategyRegistry(List.of(lazy));

        assertThat(registry.get(null)).isSameAs(lazy);
        assertThat(registry.get("")).isSameAs(lazy);
    }

    @Test
    @DisplayName("전략명으로 등록된 전략을 찾는다")
    void shouldResolveByName() {
        StubOrderLoadingStrategy fetchJoin = new StubOrderLoadingStrategy("fetch-join");
        OrderLoadingStrategyRegistry registry = new OrderLoadingStrategyRegistry(List.of(
                new StubOrderLoadingStrategy("lazy"),
                fetchJoin
        ));

        assertThat(registry.get("fetch-join")).isSameAs(fetchJoin);
    }

    @Test
    @DisplayName("알 수 없는 전략명은 예외를 던진다")
    void shouldRejectUnknownName() {
        OrderLoadingStrategyRegistry registry = new OrderLoadingStrategyRegistry(List.of(
                new StubOrderLoadingStrategy("lazy")
        ));

        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported order loading strategy: unknown");
    }

    @Test
    @DisplayName("중복 전략명은 registry 생성 시점에 실패한다")
    void shouldRejectDuplicateNames() {
        assertThatThrownBy(() -> new OrderLoadingStrategyRegistry(List.of(
                new StubOrderLoadingStrategy("lazy"),
                new StubOrderLoadingStrategy("lazy")
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate order loading strategy: lazy");
    }

    private record StubOrderLoadingStrategy(String name) implements OrderLoadingStrategy {
        @Override
        public List<OrderResponse> loadByUserId(Long userId) {
            return List.of();
        }
    }
}
```

- [ ] **Step 2: Run registry test and verify it fails**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*OrderLoadingStrategyRegistryTest"
```

Expected:

- fails because `OrderLoadingStrategy` and `OrderLoadingStrategyRegistry` do not exist yet

- [ ] **Step 3: Create `OrderLoadingStrategy` contract**

Create `ecommerce/src/main/java/com/dblab/ecommerce/service/order/OrderLoadingStrategy.java`:

```java
package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;

import java.util.List;

public interface OrderLoadingStrategy {
    String name();

    List<OrderResponse> loadByUserId(Long userId);
}
```

- [ ] **Step 4: Create `OrderLoadingStrategyRegistry`**

Create `ecommerce/src/main/java/com/dblab/ecommerce/service/order/OrderLoadingStrategyRegistry.java`:

```java
package com.dblab.ecommerce.service.order;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderLoadingStrategyRegistry {

    private static final String DEFAULT_STRATEGY = "lazy";

    private final Map<String, OrderLoadingStrategy> strategies;

    public OrderLoadingStrategyRegistry(List<OrderLoadingStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        OrderLoadingStrategy::name,
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate order loading strategy: " + left.name());
                        }
                ));
    }

    public OrderLoadingStrategy get(String name) {
        String strategyName = name == null || name.isBlank() ? DEFAULT_STRATEGY : name;
        OrderLoadingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported order loading strategy: " + strategyName);
        }
        return strategy;
    }
}
```

- [ ] **Step 5: Create order loading strategy classes**

Create `LazyOrderLoadingStrategy.java`:

```java
package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LazyOrderLoadingStrategy implements OrderLoadingStrategy {

    private final OrderRepository orderRepository;

    @Override
    public String name() {
        return "lazy";
    }

    @Override
    public List<OrderResponse> loadByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}
```

Create `FetchJoinOrderLoadingStrategy.java`:

```java
package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FetchJoinOrderLoadingStrategy implements OrderLoadingStrategy {

    private final OrderRepository orderRepository;

    @Override
    public String name() {
        return "fetch-join";
    }

    @Override
    public List<OrderResponse> loadByUserId(Long userId) {
        return orderRepository.findByUserIdWithFetchJoin(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}
```

Create `BatchSizeOrderLoadingStrategy.java`:

```java
package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BatchSizeOrderLoadingStrategy implements OrderLoadingStrategy {

    private final OrderRepository orderRepository;

    @Override
    public String name() {
        return "batch-size";
    }

    @Override
    public List<OrderResponse> loadByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}
```

Create `EntityGraphOrderLoadingStrategy.java`:

```java
package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EntityGraphOrderLoadingStrategy implements OrderLoadingStrategy {

    private final OrderRepository orderRepository;

    @Override
    public String name() {
        return "entity-graph";
    }

    @Override
    public List<OrderResponse> loadByUserId(Long userId) {
        return orderRepository.findGraphByUserId(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}
```

- [ ] **Step 6: Refactor `OrderService` to delegate to registry**

Replace `OrderService` with:

```java
package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.service.order.OrderLoadingStrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderLoadingStrategyRegistry orderLoadingStrategyRegistry;

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return getOrdersByUserId(userId, "lazy");
    }

    public List<OrderResponse> getOrdersByUserId(Long userId, String strategyName) {
        return orderLoadingStrategyRegistry.get(strategyName).loadByUserId(userId);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategyName strategyName) {
        return getOrdersByUserId(userId, strategyName.value());
    }
}
```

- [ ] **Step 7: Update `OrderController` to pass strategy string**

Replace controller method body with:

```java
@GetMapping
public List<OrderResponse> getOrders(
        @RequestParam Long userId,
        @RequestParam(required = false) String strategy) {
    return orderService.getOrdersByUserId(userId, strategy);
}
```

Remove `OrderLoadingStrategyName` import if it is no longer used.

- [ ] **Step 8: Run order focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*OrderLoadingStrategyNameTest" --tests "*OrderLoadingStrategyRegistryTest" --tests "*OrderRepositoryTest"
```

Expected:

- command exits 0
- `OrderService` has no `switch (strategy)` block

- [ ] **Step 9: Verify no order service switch remains**

Run:

```bash
rtk grep "switch \\(strategy\\)|case LAZY|case FETCH_JOIN|case BATCH_SIZE|case ENTITY_GRAPH" ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java
```

Expected:

- no matches

- [ ] **Step 10: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/service ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java ecommerce/src/test/java/com/dblab/ecommerce/service ecommerce/src/test/java/com/dblab/ecommerce/service/order
git commit -m "refactor(order): use loading strategy registry"
```
