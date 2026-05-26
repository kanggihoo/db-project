# Phase 5 Scope

Phase 5 validates whether QueryDSL DTO projection improves the product search read path while preserving a comparable baseline path for learning evidence.

## In Scope

- `GET /api/products`
- `strategy=baseline|querydsl`
- omitted `strategy` defaults to `querydsl`
- optional `categoryId` and `status` request parameters
- baseline path: Spring Data JPA entity query, then `ProductResponse.from`
- QueryDSL path: `ProductResponse` projection
- row-by-row update vs JPQL bulk update comparison
- evidence stored under [docs/evidence/phase-05](../../evidence/phase-05/README.md)

## Out of Scope

- `minPrice`, `maxPrice`, and `keyword` filters
- required k6 or Grafana evidence
- stock, coupon, or order concurrency behavior
- locks, retry, and idempotency strategies
- other user-facing product search API changes

## Completion Checklist

- [ ] Product search baseline evidence captured
- [ ] QueryDSL DTO projection evidence captured
- [ ] Product search comparison summarized
- [ ] Row-by-row update evidence captured
- [ ] JPQL bulk update evidence captured
- [ ] Bulk update comparison summarized
- [ ] Phase report finalized with links to evidence
