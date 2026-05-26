# Phase 05 Evidence

Phase 5 evidence compares baseline entity-based product search with QueryDSL DTO projection and compares row-by-row order status updates with JPQL bulk update.

## Product Search

| Evidence | Path | Result |
|---|---|---|
| Measurement condition | [product-search/measurement-condition.md](./product-search/measurement-condition.md) | `categoryId=200`, `status=ON_SALE`, PostgreSQL Testcontainers |
| Baseline SQL | [product-search/baseline-sql.txt](./product-search/baseline-sql.txt) | Product entity columns selected before `ProductResponse.from(product)` |
| QueryDSL SQL | [product-search/querydsl-sql.txt](./product-search/querydsl-sql.txt) | `ProductResponse` fields projected directly |
| Test output | [product-search/strategy-test-output.txt](./product-search/strategy-test-output.txt) | `PHASE5_PRODUCT_SEARCH_BASELINE_SQL_COUNT=1`, `PHASE5_PRODUCT_SEARCH_QUERYDSL_SQL_COUNT=1` |
| Summary | [product-search/summary.md](./product-search/summary.md) | Equal `ProductResponse` values and null QueryDSL predicates omitted |

## Bulk Update

| Evidence | Path | Result |
|---|---|---|
| Measurement condition | [bulk-update/measurement-condition.md](./bulk-update/measurement-condition.md) | `PENDING` -> `PREPARING` order status fixture |
| Row-by-row SQL count | [bulk-update/loop-update-sql-count.txt](./bulk-update/loop-update-sql-count.txt) | `PHASE5_ROW_BY_ROW_UPDATE_SQL_COUNT=4`, `entityUpdateCount=3` |
| Bulk update SQL count | [bulk-update/bulk-update-sql-count.txt](./bulk-update/bulk-update-sql-count.txt) | `PHASE5_BULK_UPDATE_SQL_COUNT=1`, `updatedRows=3` |
| Test output | [bulk-update/persistence-context-test-output.txt](./bulk-update/persistence-context-test-output.txt) | Focused integration test output |
| Summary | [bulk-update/summary.md](./bulk-update/summary.md) | Bulk update uses fewer prepared statements and clears persistence context |

## Optional Evidence

k6/Grafana and `pg_stat_statements` can be added later for broader runtime observation, but they are not required for Phase 5 closeout.
