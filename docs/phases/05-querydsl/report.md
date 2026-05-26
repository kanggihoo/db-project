# Phase 5 Report

## Status

Completed.

Phase 5 keeps the existing product search API and adds an explicit strategy comparison for learning evidence:

- `strategy=baseline`: Spring Data JPA entity query, then `ProductResponse.from(product)`
- `strategy=querydsl`: QueryDSL DTO projection
- omitted `strategy`: defaults to `querydsl`

## Product Search

Focused integration evidence shows that baseline and QueryDSL return equivalent `ProductResponse` values under shared `categoryId=200` and `status=ON_SALE` conditions.

| Strategy | Mapping path | SQL shape | SQL count evidence |
|---|---|---|---:|
| `baseline` | Product entity -> `ProductResponse.from(product)` | Product entity columns selected, including fields not needed by response | 1 |
| `querydsl` | QueryDSL projection -> `ProductResponse` | `ProductResponse` fields projected: `id`, `category_id`, `name`, `base_price`, `status` | 1 |

Evidence:

- [baseline-sql.txt](../../evidence/phase-05/product-search/baseline-sql.txt)
- [querydsl-sql.txt](../../evidence/phase-05/product-search/querydsl-sql.txt)
- [strategy-test-output.txt](../../evidence/phase-05/product-search/strategy-test-output.txt)
- [summary.md](../../evidence/phase-05/product-search/summary.md)

The QueryDSL test also verifies that null optional predicates are omitted.

## Bulk Update

Focused integration evidence compares managed entity dirty checking with JPQL bulk update under the same fixture.

| Strategy | Rows changed | Hibernate prepareStatementCount | Other evidence |
|---|---:|---:|---|
| Row-by-row dirty checking | 3 | 4 | one select + three updates, `entityUpdateCount=3` |
| JPQL bulk update | 3 | 1 | `updatedRows=3` |

Evidence:

- [loop-update-sql-count.txt](../../evidence/phase-05/bulk-update/loop-update-sql-count.txt)
- [bulk-update-sql-count.txt](../../evidence/phase-05/bulk-update/bulk-update-sql-count.txt)
- [persistence-context-test-output.txt](../../evidence/phase-05/bulk-update/persistence-context-test-output.txt)
- [summary.md](../../evidence/phase-05/bulk-update/summary.md)

The bulk repository uses `@Modifying(clearAutomatically = true, flushAutomatically = true)` with a method-level transaction boundary. Evidence records the clear/reload behavior after the bulk update: a previously loaded order is reloaded with the updated `PREPARING` state instead of the stale `PENDING` value.

## Evidence Index

All required Phase 5 evidence is organized under [docs/evidence/phase-05/README.md](../../evidence/phase-05/README.md).

k6/Grafana and `pg_stat_statements` are optional for this phase and were not required for closeout.

## Phase 6 Handoff

Phase 5 optimized and compared a simple product search read path. Phase 6 should move to Product/Review aggregate queries where the likely questions are:

- `GROUP BY` and `HAVING` behavior for product review summaries
- expression indexes for aggregate/filter expressions
- execution plan comparison before and after indexing
- whether indexes improve aggregate queries as clearly as they improve simple predicates
