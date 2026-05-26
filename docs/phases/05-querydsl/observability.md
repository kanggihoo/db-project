# Phase 5 Observability

Phase 5 evidence is small, repeatable, and tied to the comparison being measured.

## Required Signals

- representative SQL for `GET /api/products?strategy=baseline`
- representative SQL for `GET /api/products?strategy=querydsl`
- Hibernate statistics, especially `prepareStatementCount`
- focused integration test output
- short summaries that explain query count, selected columns, and mapping path

## Product Search Interpretation

Baseline search loads `Product` entity columns and converts each entity through `ProductResponse.from(product)`.

QueryDSL search projects directly into `ProductResponse` fields:

- `id`
- `categoryId`
- `name`
- `basePrice`
- `status`

Both strategies returned equivalent `ProductResponse` values under the shared fixture conditions and both used one SQL statement. The meaningful Phase 5 difference is SQL shape and mapping path, not k6 throughput.

QueryDSL optional predicates are expected to be omitted when `categoryId` or `status` is null. Focused tests verify this behavior.

## Bulk Update Interpretation

The row-by-row update comparison records the SQL count and Hibernate entity update count for managed entity dirty checking:

- `prepareStatementCount=4`
- one select plus three updates under the fixture
- `entityUpdateCount=3`

The JPQL bulk update comparison records the reduced SQL count:

- `prepareStatementCount=1`
- `updatedRows=3`
- `@Modifying(clearAutomatically = true, flushAutomatically = true)` flushes pending changes and clears the persistence context after the bulk update.

## Optional Signals

- `EXPLAIN` output for representative product search SQL
- `pg_stat_statements` summaries
- k6 summaries
- Grafana screenshots

k6, Grafana, and `pg_stat_statements` are useful context when available, but they are not required evidence for Phase 5 completion.
