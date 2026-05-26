# Phase 5 Scope

Phase 5 validates whether QueryDSL DTO projection improves the product search read path while preserving a comparable baseline path for learning evidence.

## In Scope

- `GET /api/products`
- `strategy=baseline|querydsl`
- omitted `strategy` defaults to `querydsl`
- optional `categoryId` and `status` request parameters
- baseline path: Spring Data JPA entity query, then `ProductResponse.from(product)`
- QueryDSL path: `ProductResponse` DTO projection
- QueryDSL null predicate omission for optional conditions
- row-by-row update vs JPQL bulk update comparison
- evidence stored under [docs/evidence/phase-05](../../evidence/phase-05/README.md)

## Out of Scope

- `minPrice`, `maxPrice`, and `keyword` filters
- required k6 or Grafana evidence
- required `pg_stat_statements` evidence
- stock, coupon, or order concurrency behavior
- locks, retry, and idempotency strategies
- other user-facing product search API changes

## Completion Checklist

- [x] `GET /api/products` accepts `strategy=baseline|querydsl`
- [x] omitted `strategy` defaults to `querydsl`
- [x] baseline and QueryDSL return equal `ProductResponse` values under shared `categoryId` and `status` conditions
- [x] QueryDSL null predicates are omitted safely in focused tests
- [x] baseline SQL shape and QueryDSL SQL shape are captured
- [x] row-by-row update vs bulk update SQL count evidence is captured
- [x] bulk update persistence context behavior is verified
- [x] evidence is organized under `docs/evidence/phase-05/`
- [x] Phase 6 handoff is recorded in the report
