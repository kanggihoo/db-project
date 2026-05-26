# Phase 5. QueryDSL Product Search

Phase 5 compares the current Product entity search baseline with a QueryDSL DTO projection through the same product search API.

## Current Status

Phase 5 is in progress.

- API contract: `GET /api/products`
- Strategy parameter: `strategy=baseline|querydsl`
- Default behavior: omitted `strategy` uses `querydsl`
- This slice only adds the documentation scaffold and API/service contract.
- QueryDSL repository implementation and measurements are handled in later slices.
- k6 and Grafana are optional references, not required evidence for Phase 5.

## Documents

| Document | Purpose |
|---|---|
| [scope.md](./scope.md) | Phase 5 scope, exclusions, and completion checklist |
| [runbook.md](./runbook.md) | Repeatable commands for focused tests and evidence capture |
| [observability.md](./observability.md) | SQL, Hibernate statistics, and optional observation notes |
| [report.md](./report.md) | Initial in-progress report and measurement status |

## Source Documents

- Roadmap: [docs/roadmap/06-phase-5-querydsl.md](../../roadmap/06-phase-5-querydsl.md)
- Design spec: [docs/superpowers/specs/2026-05-26-phase-5-querydsl-design.md](../../superpowers/specs/2026-05-26-phase-5-querydsl-design.md)
- Phase 4 report: [docs/phases/04-transaction-isolation/report.md](../04-transaction-isolation/report.md)

## Evidence

- Planned Phase evidence, to be created in slice 003: [docs/evidence/phase-05/README.md](../../evidence/phase-05/README.md)
