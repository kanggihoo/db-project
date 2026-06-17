# 999 Integration Stabilization

## Goal

Query strategy refactor 전체가 Phase 3/5/7 API, tests, k6/Grafana 계약을 유지하는지 최종 점검한다.

## Files

- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/service/**/*.java`
- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/controller/*.java`
- Inspect: `ecommerce/src/test/java/com/dblab/ecommerce/**/*.java`
- Inspect: `k6/*.js`
- Inspect: `docs/superpowers/specs/2026-06-02-query-strategy-refactor-design.md`
- Inspect: `docs/superpowers/plans/query-strategy-refactor/`

## Steps

- [ ] **Step 1: Verify no service switch hot spots remain**

Run:

```bash
rtk grep "switch \\(strategy\\)|case LAZY|case FETCH_JOIN|case BATCH_SIZE|case ENTITY_GRAPH|case BASELINE|case QUERYDSL" ecommerce/src/main/java/com/dblab/ecommerce/service
```

Expected:

- no matches in `OrderService.java` or `ProductService.java`
- enum `from` methods may still use stream/filter logic, not switch execution

- [ ] **Step 2: Verify strategy/reader files exist**

Run:

```bash
rtk proxy find ecommerce/src/main/java/com/dblab/ecommerce/service -type f | sort
```

Expected output includes:

```text
ecommerce/src/main/java/com/dblab/ecommerce/service/order/OrderLoadingStrategy.java
ecommerce/src/main/java/com/dblab/ecommerce/service/order/OrderLoadingStrategyRegistry.java
ecommerce/src/main/java/com/dblab/ecommerce/service/order/LazyOrderLoadingStrategy.java
ecommerce/src/main/java/com/dblab/ecommerce/service/order/FetchJoinOrderLoadingStrategy.java
ecommerce/src/main/java/com/dblab/ecommerce/service/order/BatchSizeOrderLoadingStrategy.java
ecommerce/src/main/java/com/dblab/ecommerce/service/order/EntityGraphOrderLoadingStrategy.java
ecommerce/src/main/java/com/dblab/ecommerce/service/product/ProductSearchStrategy.java
ecommerce/src/main/java/com/dblab/ecommerce/service/product/ProductSearchStrategyRegistry.java
ecommerce/src/main/java/com/dblab/ecommerce/service/product/BaselineProductSearchStrategy.java
ecommerce/src/main/java/com/dblab/ecommerce/service/product/QuerydslProductSearchStrategy.java
ecommerce/src/main/java/com/dblab/ecommerce/service/point/OffsetPointPaginationReader.java
ecommerce/src/main/java/com/dblab/ecommerce/service/point/CursorPointPaginationReader.java
```

- [ ] **Step 3: Run focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*OrderLoadingStrategyNameTest" --tests "*OrderLoadingStrategyRegistryTest" --tests "*OrderRepositoryTest"
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyNameTest" --tests "*ProductSearchStrategyRegistryTest" --tests "*ProductSearchStrategyTest"
cd ecommerce && rtk gradlew test --tests "*PointCursorApiTest" --tests "*PointHistoryRepositoryTest"
```

Expected:

- all commands exit 0

- [ ] **Step 4: Run full Gradle test suite**

Run:

```bash
cd ecommerce && rtk gradlew test
```

Expected:

- command exits 0

- [ ] **Step 5: Verify observability contract**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected:

- command exits 0

- [ ] **Step 6: Verify formatting and unintended changes**

Run:

```bash
rtk git diff --check
rtk git status --short
```

Expected:

- no whitespace errors
- root reference specs remain untracked and unstaged unless the user explicitly asks otherwise

- [ ] **Step 7: Review diff for scope creep**

Run:

```bash
rtk git diff
```

Check for unintended changes:

- API endpoint path changes
- request parameter value changes
- k6 script or preset JSON changes
- Grafana dashboard YAML changes
- Phase Evidence file moves
- Phase 11 concurrency implementation added early
- one generic Point pagination registry that hides Offset/Cursor response differences

- [ ] **Step 8: Commit final refactor**

If previous slices were not committed individually, commit the complete implementation:

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce ecommerce/src/test/java/com/dblab/ecommerce docs/superpowers/specs/2026-06-02-query-strategy-refactor-design.md docs/superpowers/plans/query-strategy-refactor
git commit -m "refactor(query): split phase query strategies"
```

If previous slices were committed individually, commit only remaining docs or stabilization fixes:

```bash
git add docs/superpowers/specs/2026-06-02-query-strategy-refactor-design.md docs/superpowers/plans/query-strategy-refactor
git commit -m "docs(query): add strategy refactor plan"
```
