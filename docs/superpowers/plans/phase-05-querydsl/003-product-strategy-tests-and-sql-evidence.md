# 003 Product Strategy Tests And SQL Evidence

## Goal

Add focused tests proving baseline/querydsl product search equivalence, default QueryDSL behavior, optional predicate handling, and SQL count difference.

## Files

- Create: `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java`
- Modify: `ecommerce/src/test/resources/test-data/product-setup.sql`
- Create: `docs/evidence/phase-05/README.md`
- Create: `docs/evidence/phase-05/product-search/measurement-condition.md`
- Create: `docs/evidence/phase-05/product-search/baseline-sql.txt`
- Create: `docs/evidence/phase-05/product-search/querydsl-sql.txt`
- Create: `docs/evidence/phase-05/product-search/strategy-test-output.txt`
- Create: `docs/evidence/phase-05/product-search/summary.md`

## Steps

- [ ] **Step 1: Extend product fixture with a second category and deleted product**

Append to `ecommerce/src/test/resources/test-data/product-setup.sql`:

```sql

-- 4. 다른 카테고리 상품 1건
INSERT INTO category (id, name, depth) VALUES (201, '생활용품-SQL', 0);
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (207, 201, '다른카테고리상품0', 20000, 'ON_SALE', false, NOW(), NOW());

-- 5. 삭제 상품 1건: Phase 5에서는 is_deleted 필터를 추가하지 않는다.
INSERT INTO product (id, category_id, name, base_price, status, is_deleted, created_at, updated_at)
VALUES (208, 200, '삭제상품0', 30000, 'ON_SALE', true, NOW(), NOW());
```

- [ ] **Step 2: Create product strategy test package**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'ecommerce/src/test/java/com/dblab/ecommerce/product' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 3: Add `ProductSearchStrategyTest`**

Create `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java`:

```java
package com.dblab.ecommerce.product;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.config.QuerydslConfig;
import com.dblab.ecommerce.repository.ProductQueryRepository;
import com.dblab.ecommerce.service.ProductSearchStrategy;
import com.dblab.ecommerce.service.ProductService;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        TestcontainersConfiguration.class,
        QuerydslConfig.class,
        ProductQueryRepository.class,
        ProductService.class
})
@Sql("/test-data/product-setup.sql")
class ProductSearchStrategyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("baseline과 querydsl은 같은 categoryId/status 조건에서 같은 응답을 반환한다")
    void baselineAndQuerydslReturnSameResponsesForSharedConditions() {
        List<ProductResponse> baseline = productService.searchProducts(
                200L, Product.Status.ON_SALE, ProductSearchStrategy.BASELINE);
        List<ProductResponse> querydsl = productService.searchProducts(
                200L, Product.Status.ON_SALE, ProductSearchStrategy.QUERYDSL);

        assertThat(sorted(querydsl)).isEqualTo(sorted(baseline));
    }

    @Test
    @DisplayName("strategy 생략 경로는 querydsl과 같은 응답을 반환한다")
    void omittedStrategyDefaultsToQuerydsl() {
        List<ProductResponse> omitted = productService.searchProducts(200L, Product.Status.SOLD_OUT);
        List<ProductResponse> querydsl = productService.searchProducts(
                200L, Product.Status.SOLD_OUT, ProductSearchStrategy.QUERYDSL);

        assertThat(sorted(omitted)).isEqualTo(sorted(querydsl));
    }

    @Test
    @DisplayName("QueryDSL은 categoryId null 조건을 제외하고 status만으로 조회한다")
    void querydslOmitsNullCategoryCondition() {
        List<ProductResponse> result = productService.searchProducts(
                null, Product.Status.SOLD_OUT, ProductSearchStrategy.QUERYDSL);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(product -> product.status() == Product.Status.SOLD_OUT);
    }

    @Test
    @DisplayName("QueryDSL은 status null 조건을 제외하고 categoryId만으로 조회한다")
    void querydslOmitsNullStatusCondition() {
        List<ProductResponse> result = productService.searchProducts(
                201L, null, ProductSearchStrategy.QUERYDSL);

        assertThat(result).hasSize(1);
        assertThat(result).allMatch(product -> product.categoryId().equals(201L));
    }

    @Test
    @DisplayName("QueryDSL projection과 baseline entity 조회는 공유 조건에서 각각 하나의 SQL statement를 사용한다")
    void querydslProjectionUsesSingleStatementLikeBaseline() {
        Statistics statistics = statistics();

        statistics.clear();
        productService.searchProducts(200L, Product.Status.ON_SALE, ProductSearchStrategy.BASELINE);
        long baselineStatements = statistics.getPrepareStatementCount();

        statistics.clear();
        productService.searchProducts(200L, Product.Status.ON_SALE, ProductSearchStrategy.QUERYDSL);
        long querydslStatements = statistics.getPrepareStatementCount();

        assertThat(baselineStatements).isEqualTo(1);
        assertThat(querydslStatements).isEqualTo(1);

        System.out.println("PHASE5_PRODUCT_SEARCH_BASELINE_SQL_COUNT=" + baselineStatements);
        System.out.println("PHASE5_PRODUCT_SEARCH_QUERYDSL_SQL_COUNT=" + querydslStatements);
    }

    private Statistics statistics() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }

    private List<ProductResponse> sorted(List<ProductResponse> responses) {
        return responses.stream()
                .sorted(Comparator.comparing(ProductResponse::productId))
                .toList();
    }
}
```

- [ ] **Step 4: Run product strategy tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyTest"
```

Expected: all `ProductSearchStrategyTest` tests pass.

- [ ] **Step 5: Create product search evidence directories**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'docs/evidence/phase-05/product-search' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 6: Add product search measurement condition**

Create `docs/evidence/phase-05/product-search/measurement-condition.md`:

```markdown
# Product Search Measurement Condition

| Field | Value |
|---|---|
| Phase | 05-querydsl |
| Scenario | product-search |
| Database | PostgreSQL Testcontainers |
| Fixture | `ecommerce/src/test/resources/test-data/product-setup.sql` |
| Baseline strategy | `baseline` |
| QueryDSL strategy | `querydsl` |
| Shared conditions | `categoryId=200`, `status=ON_SALE` |
| Required tools | JUnit, Hibernate statistics, Hibernate SQL logs |
| k6/Grafana | Not used |
```

- [ ] **Step 7: Add representative baseline SQL**

Create `docs/evidence/phase-05/product-search/baseline-sql.txt` with the SQL captured from Hibernate logs. The expected shape is:

```text
Baseline representative SQL

select
    p1_0.id,
    p1_0.base_price,
    p1_0.category_id,
    p1_0.created_at,
    p1_0.description,
    p1_0.is_deleted,
    p1_0.name,
    p1_0.status,
    p1_0.updated_at
from
    product p1_0
where
    p1_0.category_id=?
    and p1_0.status=?
```

- [ ] **Step 8: Add representative QueryDSL SQL**

Create `docs/evidence/phase-05/product-search/querydsl-sql.txt` with the SQL captured from Hibernate logs. The expected shape is:

```text
QueryDSL representative SQL

select
    p1_0.id,
    p1_0.category_id,
    p1_0.name,
    p1_0.base_price,
    p1_0.status
from
    product p1_0
where
    p1_0.category_id=?
    and p1_0.status=?
```

- [ ] **Step 9: Capture product strategy test output**

Run from `ecommerce/`:

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest" > ..\docs\evidence\phase-05\product-search\strategy-test-output.txt
```

Expected: command exits 0 and `strategy-test-output.txt` contains the Gradle success output.

- [ ] **Step 10: Add product search summary**

Create `docs/evidence/phase-05/product-search/summary.md`:

```markdown
# Product Search Summary

## Result

The baseline and QueryDSL strategies returned equivalent `ProductResponse` values for shared `categoryId` and `status` conditions.

## SQL Shape

- baseline selected Product entity columns before mapping to `ProductResponse`.
- querydsl selected only the fields needed by `ProductResponse`.

## SQL Count

Both strategies used one SQL statement for the shared condition set. The Phase 5 difference is selected column shape and DTO projection, not load-test throughput.

## Null Conditions

QueryDSL omitted null `categoryId` and null `status` predicates in focused tests.
```

- [ ] **Step 11: Add phase evidence index**

Create `docs/evidence/phase-05/README.md`:

```markdown
# Phase 05 Evidence

Phase 5 evidence compares baseline entity-based product search with QueryDSL DTO projection and compares row-by-row order status updates with JPQL bulk update.

## Product Search

| Evidence | Path |
|---|---|
| Measurement condition | [product-search/measurement-condition.md](./product-search/measurement-condition.md) |
| Baseline SQL | [product-search/baseline-sql.txt](./product-search/baseline-sql.txt) |
| QueryDSL SQL | [product-search/querydsl-sql.txt](./product-search/querydsl-sql.txt) |
| Test output | [product-search/strategy-test-output.txt](./product-search/strategy-test-output.txt) |
| Summary | [product-search/summary.md](./product-search/summary.md) |

## Bulk Update

Bulk update evidence is added in slice 004.
```

- [ ] **Step 12: Commit**

```bash
git add ecommerce/src/test/java/com/dblab/ecommerce/product ecommerce/src/test/resources/test-data/product-setup.sql docs/evidence/phase-05
git commit -m "test(phase5): compare product search strategies"
```
