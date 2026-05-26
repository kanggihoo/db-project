# E-Commerce DB Optimization Learning Roadmap
## Phase 5. Query Optimization + QueryDSL

> "Fetch only what the screen needs, and write complex dynamic queries in a type-safe way."

### Current Code And Phase Connection

- Phase 3 compared the `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` read path for N+1 and loading strategy. QueryDSL DTO projection remained as Phase 5 work.
- Phase 4 documented transaction isolation and concurrent update conflicts. Phase 5 is not a data consistency phase; it focuses on read-layer DTO projection and dynamic condition composition.
- `ecommerce/build.gradle` already has QueryDSL 5.1.0 Jakarta dependencies, so Phase 5 focuses on generated Q-class usage, product search implementation, and evidence.
- The existing product search API is kept. Phase 5 exposes `strategy=baseline|querydsl` on `GET /api/products` for learning evidence.
- `baseline` keeps the Spring Data JPA entity query and maps each entity through `ProductResponse.from(product)`.
- `querydsl` uses QueryDSL DTO projection into `ProductResponse` fields and is the default when `strategy` is omitted.

### Optimization Targets

- Entity query followed by DTO mapping -> DTO projection to avoid unneeded selected columns and entity materialization.
- String-based or derived query limits for dynamic search -> QueryDSL type-safe condition composition.
- Row-by-row status updates -> JPQL bulk update to reduce prepared statement count.

Atomic update, pessimistic lock, optimistic lock, retry, and idempotency strategies for stock/coupon concurrency remain Phase 11 scope. Phase 5 bulk update evidence is limited to bulk state change and persistence context behavior.

### Experiment 1: DTO Projection

```java
// Before: full entity load, including columns not needed by the response
List<Product> products = productRepository.findByCategoryIdAndStatus(categoryId, status);
List<ProductResponse> responses = products.stream()
    .map(ProductResponse::from)
    .toList();

// After: QueryDSL projection selects only response fields
QProduct product = QProduct.product;

List<ProductResponse> responses = queryFactory
    .select(Projections.constructor(ProductResponse.class,
        product.id,
        product.categoryId,
        product.name,
        product.basePrice,
        product.status))
    .from(product)
    .where(
        categoryEq(categoryId),
        statusEq(status)
    )
    .fetch();
```

### Experiment 2: Product Search Strategy

The implemented comparison uses the existing `GET /api/products` API:

- `GET /api/products?strategy=baseline&categoryId=200&status=ON_SALE`
- `GET /api/products?strategy=querydsl&categoryId=200&status=ON_SALE`
- `GET /api/products?categoryId=200&status=ON_SALE` defaults to `querydsl`

Both strategies return equivalent `ProductResponse` values under shared `categoryId` and `status` conditions. QueryDSL also verifies that null predicates are omitted safely.

### Experiment 3: Bulk Update

```java
// Before: row-by-row baseline
List<Orders> orders = orderRepository.findByStatus(Orders.Status.PENDING);
orders.forEach(Orders::markPreparing);
entityManager.flush();

// After: bulk update
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE Orders o SET o.status = :newStatus WHERE o.status = :oldStatus")
int bulkUpdateStatus(@Param("oldStatus") Orders.Status oldStatus,
                     @Param("newStatus") Orders.Status newStatus);
```

Bulk update bypasses managed entities, so the repository uses `clearAutomatically = true` and `flushAutomatically = true`. Evidence confirms that a previously loaded order is reloaded from the database after the bulk update.

### Required Evidence

Required Phase 5 evidence is stored under `docs/evidence/phase-05/`:

- focused integration test output
- representative baseline and QueryDSL SQL
- SQL count evidence for product search and bulk update
- short product-search and bulk-update summaries

k6/Grafana and `pg_stat_statements` are optional references for this phase, not required closeout evidence.

### Measured Results

- Product search baseline selected Product entity columns and then mapped through `ProductResponse.from(product)`.
- Product search QueryDSL selected only the fields needed by `ProductResponse`.
- Both product search strategies used one SQL statement for the shared fixture condition.
- Row-by-row dirty checking used Hibernate `prepareStatementCount=4` under the fixture: one select plus three updates, with `entityUpdateCount=3`.
- JPQL bulk update changed the same three rows with Hibernate `prepareStatementCount=1`.

### Insights

- Entity loading is not always the right read model when the response needs only a subset of columns.
- QueryDSL is useful for type-safe DTO projection and optional predicate composition.
- Bulk updates reduce SQL count, but they require explicit persistence context handling.
- Read optimization and concurrency control are separate concerns; stock/coupon concurrency remains Phase 11 scope.

### Remaining Question -> Phase 6

> "Simple reads are optimized, but GROUP BY aggregate queries are still slow. Do indexes also affect Product/Review aggregate query performance?"

Phase 6 should continue with Product/Review aggregate queries, `GROUP BY`, `HAVING`, expression indexes, and execution plan comparison.

### Completion Criteria

- [x] Existing entity query plus `ProductResponse.from(product)` baseline is preserved and compared with QueryDSL DTO projection under the same measurement condition.
- [x] `GET /api/products` supports `strategy=baseline|querydsl`.
- [x] Omitted `strategy` defaults to `querydsl`.
- [x] Baseline and QueryDSL return equal `ProductResponse` values under shared `categoryId` and `status` conditions.
- [x] QueryDSL omits null predicates safely in focused tests.
- [x] Row-by-row update and bulk update SQL counts are recorded.
- [x] Bulk update persistence context behavior is documented and verified.
- [x] Phase evidence is organized under `docs/evidence/phase-05/`.
- [x] Product/Review aggregate query follow-up is handed off to Phase 6.

---
