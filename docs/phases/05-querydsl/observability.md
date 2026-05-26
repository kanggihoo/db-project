# Phase 5 Observability

Phase 5 evidence should be small, repeatable, and tied to the comparison being measured.

## Required Signals

- representative SQL for `GET /api/products?strategy=baseline`
- representative SQL for `GET /api/products?strategy=querydsl`
- Hibernate statistics, especially `prepareStatementCount`
- focused integration test output
- short summaries that explain query count, selected columns, and mapping path

## Product Search Interpretation

Baseline search is expected to load `Product` entities and convert each entity through `ProductResponse.from`.

QueryDSL search is expected to select directly into `ProductResponse` without entity materialization once the repository slice is implemented.

During this contract slice, both strategies may produce the baseline SQL because the QueryDSL repository is not implemented yet.

## Bulk Update Interpretation

The row-by-row update comparison should record the statement count and SQL shape for entity updates.

The JPQL bulk update comparison should record the reduced statement count and note persistence context considerations.

## Optional Signals

- `EXPLAIN` output for representative product search SQL
- `pg_stat_statements` summaries
- k6 summaries
- Grafana screenshots

k6 and Grafana are useful context when available, but they are not required evidence for Phase 5 completion.
