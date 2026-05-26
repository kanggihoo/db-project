# 002 QueryDSL Product Search Repository

## Goal

Add the QueryDSL product search implementation and route `strategy=querydsl` to DTO projection.

## Files

- Create: `ecommerce/src/main/java/com/dblab/ecommerce/config/QuerydslConfig.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductQueryRepository.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductRepository.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`

## Steps

- [ ] **Step 1: Add `JPAQueryFactory` configuration**

Create `ecommerce/src/main/java/com/dblab/ecommerce/config/QuerydslConfig.java`:

```java
package com.dblab.ecommerce.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuerydslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
```

- [ ] **Step 2: Add optional baseline repository methods**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductRepository.java`:

```java
package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndStatus(Long categoryId, Product.Status status);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByStatus(Product.Status status);
}
```

The service will use `findAll()` when both conditions are omitted.

- [ ] **Step 3: Add QueryDSL product repository**

Create `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductQueryRepository.java`:

```java
package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.entity.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        QProduct product = QProduct.product;

        return queryFactory
                .select(Projections.constructor(ProductResponse.class,
                        product.id,
                        product.categoryId,
                        product.name,
                        product.basePrice,
                        product.status))
                .from(product)
                .where(searchCondition(product, categoryId, status))
                .fetch();
    }

    private BooleanBuilder searchCondition(QProduct product, Long categoryId, Product.Status status) {
        BooleanBuilder builder = new BooleanBuilder();

        if (categoryId != null) {
            builder.and(product.categoryId.eq(categoryId));
        }

        if (status != null) {
            builder.and(product.status.eq(status));
        }

        return builder;
    }
}
```

- [ ] **Step 4: Route `QUERYDSL` to `ProductQueryRepository`**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`:

```java
package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductQueryRepository;
import com.dblab.ecommerce.repository.ProductRepository;
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

    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        return searchProducts(categoryId, status, ProductSearchStrategy.QUERYDSL);
    }

    public List<ProductResponse> searchProducts(
            Long categoryId,
            Product.Status status,
            ProductSearchStrategy strategy) {
        if (strategy == ProductSearchStrategy.BASELINE) {
            return searchProductsBaseline(categoryId, status);
        }

        return productQueryRepository.searchProducts(categoryId, status);
    }

    private List<ProductResponse> searchProductsBaseline(Long categoryId, Product.Status status) {
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

- [ ] **Step 5: Run compile check**

Run:

```bash
cd ecommerce && rtk gradlew compileJava
```

Expected: Gradle exits 0 and generated QueryDSL Q-types are available during compilation.

- [ ] **Step 6: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/config/QuerydslConfig.java ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductQueryRepository.java ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductRepository.java ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java
git commit -m "feat(phase5): add querydsl product search"
```
