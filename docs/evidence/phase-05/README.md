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

| Evidence | Path |
|---|---|
| Measurement condition | [bulk-update/measurement-condition.md](./bulk-update/measurement-condition.md) |
| Row-by-row SQL count | [bulk-update/loop-update-sql-count.txt](./bulk-update/loop-update-sql-count.txt) |
| Bulk update SQL count | [bulk-update/bulk-update-sql-count.txt](./bulk-update/bulk-update-sql-count.txt) |
| Test output | [bulk-update/persistence-context-test-output.txt](./bulk-update/persistence-context-test-output.txt) |
| Summary | [bulk-update/summary.md](./bulk-update/summary.md) |
