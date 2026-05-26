# Phase 5 Runbook

Run commands from `ecommerce/` unless noted otherwise.

## Focused Test Commands

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest"
rtk gradlew test --tests "*OrderBulkUpdateTest"
rtk gradlew compileJava
```

## Evidence Capture Commands

Use focused test output and representative SQL snapshots under `docs/evidence/phase-05/`.

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest" --info > ../docs/evidence/phase-05/product-search/strategy-test-output.txt
rtk gradlew test --tests "*OrderBulkUpdateTest" --info > ../docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt
```

If SQL snapshots need to be refreshed, enable Hibernate SQL logging for the focused test run and save only the representative SQL shape.

## Expected Evidence Files

- `docs/evidence/phase-05/README.md`
- `docs/evidence/phase-05/product-search/measurement-condition.md`
- `docs/evidence/phase-05/product-search/baseline-sql.txt`
- `docs/evidence/phase-05/product-search/querydsl-sql.txt`
- `docs/evidence/phase-05/product-search/strategy-test-output.txt`
- `docs/evidence/phase-05/product-search/summary.md`
- `docs/evidence/phase-05/bulk-update/measurement-condition.md`
- `docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt`
- `docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt`
- `docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt`
- `docs/evidence/phase-05/bulk-update/summary.md`

## Closeout Checks

From the repository root:

```powershell
rtk powershell -NoProfile -Command "$paths = @('docs/evidence/phase-05/README.md','docs/evidence/phase-05/product-search/measurement-condition.md','docs/evidence/phase-05/product-search/baseline-sql.txt','docs/evidence/phase-05/product-search/querydsl-sql.txt','docs/evidence/phase-05/product-search/strategy-test-output.txt','docs/evidence/phase-05/product-search/summary.md','docs/evidence/phase-05/bulk-update/measurement-condition.md','docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt','docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt','docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt','docs/evidence/phase-05/bulk-update/summary.md'); $paths | ForEach-Object { [pscustomobject]@{ Path = $_; Exists = Test-Path $_ } }"
rtk rg -n "Phase 5|phase-05|05-querydsl|QueryDSL|strategy" docs/roadmap/06-phase-5-querydsl.md docs/phases/05-querydsl docs/evidence/phase-05
```
