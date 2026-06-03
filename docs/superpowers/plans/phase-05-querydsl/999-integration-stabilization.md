# 999 Integration Stabilization

## Goal

Run final verification for Phase 5 and fix integration or documentation drift before closing the branch.

## Files

- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`
- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`
- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductQueryRepository.java`
- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java`
- Inspect: `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java`
- Inspect: `ecommerce/src/test/java/com/dblab/ecommerce/order/OrderBulkUpdateTest.java`
- Inspect: `docs/phases/05-querydsl/`
- Inspect: `docs/evidence/phase-05/`

## Steps

- [ ] **Step 1: Run focused Phase 5 tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyTest" --tests "*OrderBulkUpdateTest"
```

Expected: Gradle exits 0 and both focused test classes pass.

- [ ] **Step 2: Run existing repository tests touched by shared repositories**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*ProductRepositoryTest" --tests "*OrderRepositoryTest"
```

Expected: Gradle exits 0. Existing product and order repository tests still pass after repository method additions.

- [ ] **Step 3: Run compile and full tests if focused tests pass**

Run:

```bash
cd ecommerce && rtk gradlew test
```

Expected: Gradle exits 0.

- [ ] **Step 4: Check documentation files are limited to standard Phase 5 phase docs**

Run:

```bash
rtk proxy powershell -NoProfile -Command "(Get-ChildItem 'docs/phases/05-querydsl' -File).Name | Sort-Object"
```

Expected output:

```text
observability.md
README.md
report.md
runbook.md
scope.md
```

- [ ] **Step 5: Check no forbidden Phase 5 search conditions were added**

Run:

```bash
rtk rg -n "minPrice|maxPrice|keyword" ecommerce/src/main/java ecommerce/src/test/java docs/phases/05-querydsl docs/evidence/phase-05
```

Expected: no output. The terms may remain in design/spec documents, but not in implementation or active Phase 5 docs.

- [ ] **Step 6: Check required strategy values are documented and tested**

Run:

```bash
rtk rg -n "BASELINE|QUERYDSL|baseline|querydsl|strategy" ecommerce/src/main/java ecommerce/src/test/java docs/phases/05-querydsl docs/evidence/phase-05
```

Expected: output includes `ProductSearchStrategy`, controller parameter, product strategy tests, and evidence docs.

- [ ] **Step 7: Verify git diff is intentional**

Run:

```bash
rtk git status --short
rtk git diff --stat
```

Expected: only Phase 5 code, tests, docs, and evidence files are modified.

- [ ] **Step 8: Commit stabilization fixes if any were needed**

If Step 1 through Step 7 required fixes, commit only those fixes:

```bash
git add ecommerce docs
git commit -m "chore(phase5): stabilize querydsl phase"
```

If no fixes were needed, do not create an empty commit.
