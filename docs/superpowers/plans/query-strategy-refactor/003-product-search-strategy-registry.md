# 003 Product Search Strategy Registry

## Goal

Phase 5 Product search 전략을 Spring strategy bean + registry 구조로 분리하고 `ProductService`의 switch를 제거한다.

## Files

- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/product/ProductSearchStrategy.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/product/ProductSearchStrategyRegistry.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/product/BaselineProductSearchStrategy.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/product/QuerydslProductSearchStrategy.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`
- Create: `ecommerce/src/test/java/com/dblab/ecommerce/service/product/ProductSearchStrategyRegistryTest.java`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java`

## Steps

- [ ] **Step 1: Add registry unit test**

Create `ecommerce/src/test/java/com/dblab/ecommerce/service/product/ProductSearchStrategyRegistryTest.java`:

```java
package com.dblab.ecommerce.service.product;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSearchStrategyRegistryTest {

    @Test
    @DisplayName("빈 전략명은 querydsl 전략으로 처리한다")
    void shouldResolveBlankNameToQuerydsl() {
        StubProductSearchStrategy querydsl = new StubProductSearchStrategy("querydsl");
        ProductSearchStrategyRegistry registry = new ProductSearchStrategyRegistry(List.of(querydsl));

        assertThat(registry.get(null)).isSameAs(querydsl);
        assertThat(registry.get("")).isSameAs(querydsl);
    }

    @Test
    @DisplayName("전략명 대소문자를 무시하고 등록된 전략을 찾는다")
    void shouldResolveByNameIgnoringCase() {
        StubProductSearchStrategy baseline = new StubProductSearchStrategy("baseline");
        ProductSearchStrategyRegistry registry = new ProductSearchStrategyRegistry(List.of(
                baseline,
                new StubProductSearchStrategy("querydsl")
        ));

        assertThat(registry.get("BASELINE")).isSameAs(baseline);
    }

    @Test
    @DisplayName("알 수 없는 전략명은 예외를 던진다")
    void shouldRejectUnknownName() {
        ProductSearchStrategyRegistry registry = new ProductSearchStrategyRegistry(List.of(
                new StubProductSearchStrategy("querydsl")
        ));

        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported product search strategy: unknown");
    }

    @Test
    @DisplayName("중복 전략명은 registry 생성 시점에 실패한다")
    void shouldRejectDuplicateNames() {
        assertThatThrownBy(() -> new ProductSearchStrategyRegistry(List.of(
                new StubProductSearchStrategy("querydsl"),
                new StubProductSearchStrategy("QUERYDSL")
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate product search strategy: querydsl");
    }

    private record StubProductSearchStrategy(String name) implements ProductSearchStrategy {
        @Override
        public List<ProductResponse> search(Long categoryId, Product.Status status) {
            return List.of();
        }
    }
}
```

- [ ] **Step 2: Run registry test and verify it fails**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyRegistryTest"
```

Expected:

- fails because `ProductSearchStrategy` and `ProductSearchStrategyRegistry` do not exist yet

- [ ] **Step 3: Create `ProductSearchStrategy` contract**

Create `ecommerce/src/main/java/com/dblab/ecommerce/service/product/ProductSearchStrategy.java`:

```java
package com.dblab.ecommerce.service.product;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;

import java.util.List;

public interface ProductSearchStrategy {
    String name();

    List<ProductResponse> search(Long categoryId, Product.Status status);
}
```

- [ ] **Step 4: Create `ProductSearchStrategyRegistry`**

Create `ecommerce/src/main/java/com/dblab/ecommerce/service/product/ProductSearchStrategyRegistry.java`:

```java
package com.dblab.ecommerce.service.product;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProductSearchStrategyRegistry {

    private static final String DEFAULT_STRATEGY = "querydsl";

    private final Map<String, ProductSearchStrategy> strategies;

    public ProductSearchStrategyRegistry(List<ProductSearchStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        strategy -> normalize(strategy.name()),
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate product search strategy: " + normalize(left.name()));
                        }
                ));
    }

    public ProductSearchStrategy get(String name) {
        String strategyName = name == null || name.isBlank() ? DEFAULT_STRATEGY : normalize(name);
        ProductSearchStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported product search strategy: " + name);
        }
        return strategy;
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
```

- [ ] **Step 5: Create product search strategy classes**

Create `BaselineProductSearchStrategy.java`:

```java
package com.dblab.ecommerce.service.product;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BaselineProductSearchStrategy implements ProductSearchStrategy {

    private final ProductRepository productRepository;

    @Override
    public String name() {
        return "baseline";
    }

    @Override
    public List<ProductResponse> search(Long categoryId, Product.Status status) {
        return findBaselineProducts(categoryId, status)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    private List<Product> findBaselineProducts(Long categoryId, Product.Status status) {
        if (categoryId != null && status != null) {
            return productRepository.findByCategoryIdAndStatus(categoryId, status);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId);
        }
        if (status != null) {
            return productRepository.findByStatus(status);
        }
        return productRepository.findAll();
    }
}
```

Create `QuerydslProductSearchStrategy.java`:

```java
package com.dblab.ecommerce.service.product;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuerydslProductSearchStrategy implements ProductSearchStrategy {

    private final ProductQueryRepository productQueryRepository;

    @Override
    public String name() {
        return "querydsl";
    }

    @Override
    public List<ProductResponse> search(Long categoryId, Product.Status status) {
        return productQueryRepository.searchProducts(categoryId, status);
    }
}
```

- [ ] **Step 6: Refactor `ProductService` to delegate to registry**

Replace `ProductService` with:

```java
package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.dto.ProductReviewSummaryResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductReviewSummaryRepository;
import com.dblab.ecommerce.service.product.ProductSearchStrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductSearchStrategyRegistry productSearchStrategyRegistry;
    private final ProductReviewSummaryRepository productReviewSummaryRepository;

    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        return searchProducts(categoryId, status, "querydsl");
    }

    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status, String strategyName) {
        return productSearchStrategyRegistry.get(strategyName).search(categoryId, status);
    }

    public List<ProductResponse> searchProducts(
            Long categoryId,
            Product.Status status,
            ProductSearchStrategyName strategyName) {
        return searchProducts(categoryId, status, strategyName.value());
    }

    public List<ProductReviewSummaryResponse> getReviewSummary() {
        return productReviewSummaryRepository.findTopReviewSummaries();
    }
}
```

- [ ] **Step 7: Update `ProductController` to pass strategy string**

Replace `searchProducts` method body with:

```java
@GetMapping
public List<ProductResponse> searchProducts(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Product.Status status,
        @RequestParam(defaultValue = "querydsl") String strategy) {
    try {
        return productService.searchProducts(categoryId, status, strategy);
    } catch (IllegalArgumentException exception) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }
}
```

Remove the private `resolveStrategy` method and `ProductSearchStrategyName` import if unused.

- [ ] **Step 8: Update `ProductSearchStrategyTest` constructor-based test**

The existing constructor-based unit test must instantiate strategies and registry instead of passing repositories directly to `ProductService`.

Use this shape in `twoArgumentOverloadDelegatesToQuerydslRepository`:

```java
ProductRepository baselineRepository = mock(ProductRepository.class);
ProductQueryRepository queryRepository = mock(ProductQueryRepository.class);
ProductReviewSummaryRepository reviewSummaryRepository = mock(ProductReviewSummaryRepository.class);
ProductSearchStrategyRegistry registry = new ProductSearchStrategyRegistry(List.of(
        new BaselineProductSearchStrategy(baselineRepository),
        new QuerydslProductSearchStrategy(queryRepository)
));
ProductService service = new ProductService(registry, reviewSummaryRepository);
```

Keep the existing assertions:

```java
assertThat(result).isEqualTo(expected);
verify(queryRepository).searchProducts(200L, Product.Status.SOLD_OUT);
verifyNoMoreInteractions(queryRepository);
verifyNoInteractions(baselineRepository);
verifyNoInteractions(reviewSummaryRepository);
```

- [ ] **Step 9: Update `ProductSearchStrategyTest` Spring imports**

Update the `@Import` block in `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java` so Spring can create the new registry and strategy beans:

```java
@Import({
        TestcontainersConfiguration.class,
        QuerydslConfig.class,
        ProductQueryRepository.class,
        ProductReviewSummaryRepository.class,
        ProductSearchStrategyRegistry.class,
        BaselineProductSearchStrategy.class,
        QuerydslProductSearchStrategy.class,
        ProductService.class
})
```

Add imports:

```java
import com.dblab.ecommerce.service.product.BaselineProductSearchStrategy;
import com.dblab.ecommerce.service.product.ProductSearchStrategyRegistry;
import com.dblab.ecommerce.service.product.QuerydslProductSearchStrategy;
```

- [ ] **Step 10: Run product focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyNameTest" --tests "*ProductSearchStrategyRegistryTest" --tests "*ProductSearchStrategyTest"
```

Expected:

- command exits 0
- `ProductService` has no `switch (strategy)` block

- [ ] **Step 11: Verify no product service switch remains**

Run:

```bash
rtk grep "switch \\(strategy\\)|case BASELINE|case QUERYDSL|findBaselineProducts" ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java
```

Expected:

- no matches

- [ ] **Step 12: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/service ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java ecommerce/src/test/java/com/dblab/ecommerce/service/product ecommerce/src/test/java/com/dblab/ecommerce/product
git commit -m "refactor(product): use search strategy registry"
```
