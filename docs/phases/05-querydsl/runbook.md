# Phase 5 Runbook

Run commands from `ecommerce/` unless noted otherwise.

## Focused Test Commands

These are Phase 5 implementation commands to run after the relevant test slices exist. Slice 001 only defines the contract and documentation scaffold.

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest"
rtk gradlew test --tests "*OrderBulkUpdateTest"
rtk gradlew compileJava
```

## Evidence Capture Commands

Use focused test output first after the relevant test slices exist. Capture logs under `docs/evidence/phase-05/`.

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest" --info > ../docs/evidence/phase-05/product-search/strategy-test-output.txt
rtk gradlew test --tests "*OrderBulkUpdateTest" --info > ../docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt
rtk gradlew compileJava
```

If SQL snapshots are needed, enable Hibernate SQL logging for the focused test run and save the representative SQL only.

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
