# Phase 5. QueryDSL Product Search

Phase 5 compares the current Product entity search baseline with a QueryDSL DTO projection through the same product search API.

## Current Status

Phase 5 is completed.

- API contract: `GET /api/products`
- Strategy parameter: `strategy=baseline|querydsl`
- Default behavior: omitted `strategy` uses `querydsl`
- `baseline`: Spring Data JPA entity query, then `ProductResponse.from(product)`
- `querydsl`: QueryDSL DTO projection into `ProductResponse`
- Product search evidence records representative SQL shape and focused integration test output.
- Bulk update evidence records SQL count and persistence context behavior.
- k6 and Grafana are optional references, not required evidence for Phase 5 closeout.

## Documents

| Document | Purpose |
|---|---|
| [scope.md](./scope.md) | Phase 5 scope, exclusions, and completed criteria |
| [runbook.md](./runbook.md) | Repeatable commands for focused tests and evidence capture |
| [observability.md](./observability.md) | SQL, Hibernate statistics, and optional observation notes |
| [report.md](./report.md) | Final Phase 5 results and Phase 6 handoff |

## Source Documents

- Roadmap: [docs/roadmap/06-phase-5-querydsl.md](../../roadmap/06-phase-5-querydsl.md)
- Design spec: [docs/superpowers/specs/2026-05-26-phase-5-querydsl-design.md](../../superpowers/specs/2026-05-26-phase-5-querydsl-design.md)
- Phase 4 report: [docs/phases/04-transaction-isolation/report.md](../04-transaction-isolation/report.md)

## Evidence

- Phase 5 evidence index: [docs/evidence/phase-05/README.md](../../evidence/phase-05/README.md)
