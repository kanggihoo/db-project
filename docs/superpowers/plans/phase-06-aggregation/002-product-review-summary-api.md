# 002 Product Review Summary API

## Goal

Add the fixed measurement endpoint `GET /api/products/review-summary` using native SQL through `JdbcTemplate`.

## Files

- Create: `ecommerce/src/main/java/com/dblab/ecommerce/dto/ProductReviewSummaryResponse.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductReviewSummaryRepository.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`
- Create: `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductReviewSummaryTest.java`
- Create: `ecommerce/src/test/resources/test-data/phase6-review-summary-setup.sql`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java`

## Steps

- [ ] **Step 1: Add deterministic Phase 6 review summary fixture**

Create `ecommerce/src/test/resources/test-data/phase6-review-summary-setup.sql`:

```sql
INSERT INTO users (id, email, password, name, gender, grade, point_balance, created_at, updated_at)
VALUES
  (6100, 'phase6-user-0@example.com', 'pw', 'Phase6 User 0', 'MALE', 'GOLD', 0, NOW(), NOW()),
  (6101, 'phase6-user-1@example.com', 'pw', 'Phase6 User 1', 'FEMALE', 'SILVER', 0, NOW(), NOW());

INSERT INTO category (id, name, depth)
VALUES (6200, 'Phase6 Category', 0);

INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES
  (6300, 6200, 'Phase6 Product A', 10000, 'ON_SALE', false, NOW(), NOW()),
  (6301, 6200, 'Phase6 Product B', 12000, 'ON_SALE', false, NOW(), NOW()),
  (6302, 6200, 'Phase6 Product C', 15000, 'ON_SALE', false, NOW(), NOW());

INSERT INTO product_sku (id, product_id, sku_code, stock_quantity, extra_price)
VALUES
  (6400, 6300, 'PHASE6-A', 100, 0),
  (6401, 6301, 'PHASE6-B', 100, 0),
  (6402, 6302, 'PHASE6-C', 100, 0);

INSERT INTO user_address (id, user_id, address, is_default, receiver_name, receiver_phone)
VALUES
  (6500, 6100, 'Phase6 Address 0', true, 'Phase6 User 0', '010-0000-0000'),
  (6501, 6101, 'Phase6 Address 1', true, 'Phase6 User 1', '010-0000-0001');

INSERT INTO orders (id, user_id, address_id, total_price, discount_price, final_price, status, created_at)
VALUES
  (6600, 6100, 6500, 10000, 0, 10000, 'DELIVERED', NOW()),
  (6601, 6101, 6501, 12000, 0, 12000, 'DELIVERED', NOW()),
  (6602, 6100, 6500, 15000, 0, 15000, 'DELIVERED', NOW());

INSERT INTO order_item (id, order_id, sku_id, product_name, quantity, unit_price, status)
VALUES
  (6700, 6600, 6400, 'Phase6 Product A', 1, 10000, 'DELIVERED'),
  (6701, 6601, 6401, 'Phase6 Product B', 1, 12000, 'DELIVERED'),
  (6702, 6602, 6402, 'Phase6 Product C', 1, 15000, 'DELIVERED');

INSERT INTO review (id, user_id, product_id, order_item_id, rating, content, created_at)
SELECT 6800 + n, 6100, 6300, 6700, 5, 'A review ' || n, NOW()
FROM generate_series(0, 9) AS n;

INSERT INTO review (id, user_id, product_id, order_item_id, rating, content, created_at)
SELECT 6810 + n, 6101, 6301, 6701, 4, 'B review ' || n, NOW()
FROM generate_series(0, 9) AS n;

INSERT INTO review (id, user_id, product_id, order_item_id, rating, content, created_at)
SELECT 6820 + n, 6100, 6302, 6702, 3, 'C review ' || n, NOW()
FROM generate_series(0, 8) AS n;
```

- [ ] **Step 2: Add response DTO**

Create `ecommerce/src/main/java/com/dblab/ecommerce/dto/ProductReviewSummaryResponse.java`:

```java
package com.dblab.ecommerce.dto;

import java.math.BigDecimal;

public record ProductReviewSummaryResponse(
        Long productId,
        String productName,
        BigDecimal avgRating,
        Long reviewCount) {
}
```

- [ ] **Step 3: Add JdbcTemplate repository**

Create `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductReviewSummaryRepository.java`:

```java
package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.dto.ProductReviewSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class ProductReviewSummaryRepository {

    private static final String REVIEW_SUMMARY_SQL = """
            SELECT p.id AS product_id,
                   p.name AS product_name,
                   ROUND(AVG(r.rating), 2) AS avg_rating,
                   COUNT(r.id) AS review_count
            FROM product p
            LEFT JOIN review r ON r.product_id = p.id
            GROUP BY p.id, p.name
            HAVING COUNT(r.id) >= 10
            ORDER BY AVG(r.rating) DESC, p.id ASC
            LIMIT 100
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProductReviewSummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductReviewSummaryResponse> findTopReviewSummaries() {
        return jdbcTemplate.query(REVIEW_SUMMARY_SQL, (rs, rowNum) -> new ProductReviewSummaryResponse(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getObject("avg_rating", BigDecimal.class),
                rs.getLong("review_count")));
    }
}
```

- [ ] **Step 4: Add service method**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java` so it includes the new repository and method:

```java
package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.dto.ProductReviewSummaryResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductQueryRepository;
import com.dblab.ecommerce.repository.ProductRepository;
import com.dblab.ecommerce.repository.ProductReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReviewSummaryRepository productReviewSummaryRepository;

    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        return searchProducts(categoryId, status, ProductSearchStrategy.QUERYDSL);
    }

    public List<ProductResponse> searchProducts(
            Long categoryId,
            Product.Status status,
            ProductSearchStrategy strategy) {
        return switch (strategy) {
            case BASELINE -> searchProductsBaseline(categoryId, status);
            case QUERYDSL -> productQueryRepository.searchProducts(categoryId, status);
        };
    }

    public List<ProductReviewSummaryResponse> getReviewSummary() {
        return productReviewSummaryRepository.findTopReviewSummaries();
    }

    private List<ProductResponse> searchProductsBaseline(Long categoryId, Product.Status status) {
        return findBaselineProducts(categoryId, status)
                .stream().map(ProductResponse::from).toList();
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

- [ ] **Step 5: Add controller endpoint**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`:

```java
package com.dblab.ecommerce.controller;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.dto.ProductReviewSummaryResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.service.ProductSearchStrategy;
import com.dblab.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> searchProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Product.Status status,
            @RequestParam(defaultValue = "querydsl") String strategy) {
        ProductSearchStrategy searchStrategy = resolveStrategy(strategy);
        return productService.searchProducts(categoryId, status, searchStrategy);
    }

    @GetMapping("/review-summary")
    public List<ProductReviewSummaryResponse> getReviewSummary() {
        return productService.getReviewSummary();
    }

    private ProductSearchStrategy resolveStrategy(String strategy) {
        try {
            return ProductSearchStrategy.from(strategy);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
```

- [ ] **Step 6: Update existing ProductService constructor test**

Modify `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java`.

Add import:

```java
import com.dblab.ecommerce.repository.ProductReviewSummaryRepository;
```

Update `twoArgumentOverloadDelegatesToQuerydslRepository()`:

```java
@Test
@DisplayName("two-argument overload delegates to querydsl repository")
void twoArgumentOverloadDelegatesToQuerydslRepository() {
    ProductRepository baselineRepository = mock(ProductRepository.class);
    ProductQueryRepository queryRepository = mock(ProductQueryRepository.class);
    ProductReviewSummaryRepository reviewSummaryRepository = mock(ProductReviewSummaryRepository.class);
    ProductService service = new ProductService(baselineRepository, queryRepository, reviewSummaryRepository);
    List<ProductResponse> expected = List.of(
            new ProductResponse(205L, 200L, "Sold Out Product 0", 10000, Product.Status.SOLD_OUT));
    when(queryRepository.searchProducts(200L, Product.Status.SOLD_OUT)).thenReturn(expected);

    List<ProductResponse> result = service.searchProducts(200L, Product.Status.SOLD_OUT);

    assertThat(result).isEqualTo(expected);
    verify(queryRepository).searchProducts(200L, Product.Status.SOLD_OUT);
    verifyNoMoreInteractions(queryRepository);
    verifyNoInteractions(baselineRepository);
    verifyNoInteractions(reviewSummaryRepository);
}
```

- [ ] **Step 7: Add focused API test**

Create `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductReviewSummaryTest.java`:

```java
package com.dblab.ecommerce.product;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.dto.ProductReviewSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/phase6-review-summary-setup.sql")
class ProductReviewSummaryTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("review summary API returns products with at least ten reviews ordered by average rating")
    void reviewSummaryApiReturnsProductsWithAtLeastTenReviewsOrderedByAverageRating() {
        ProductReviewSummaryResponse[] response = restTemplate.getForObject(
                "/api/products/review-summary",
                ProductReviewSummaryResponse[].class);

        assertThat(response)
                .extracting(
                        ProductReviewSummaryResponse::productId,
                        ProductReviewSummaryResponse::productName,
                        ProductReviewSummaryResponse::avgRating,
                        ProductReviewSummaryResponse::reviewCount)
                .containsExactly(
                        tuple(6300L, "Phase6 Product A", new BigDecimal("5.00"), 10L),
                        tuple(6301L, "Phase6 Product B", new BigDecimal("4.00"), 10L));

        assertThat(response)
                .extracting(ProductReviewSummaryResponse::productId)
                .doesNotContain(6302L);
    }
}
```

- [ ] **Step 8: Run focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*ProductReviewSummaryTest" --tests "*ProductSearchStrategyTest"
```

Expected: Gradle exits 0.

- [ ] **Step 9: Run compile check**

Run:

```bash
cd ecommerce && rtk gradlew compileJava
```

Expected: Gradle exits 0.

- [ ] **Step 10: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/dto/ProductReviewSummaryResponse.java ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductReviewSummaryRepository.java ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java ecommerce/src/test/java/com/dblab/ecommerce/product/ProductReviewSummaryTest.java ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java ecommerce/src/test/resources/test-data/phase6-review-summary-setup.sql
git commit -m "feat(phase6): add review summary measurement API"
```
