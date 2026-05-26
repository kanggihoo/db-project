# Phase 6 Aggregation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phase 6 by measuring PostgreSQL aggregation query plans across baseline, naive index, and query-shaped index conditions, then compare one representative review summary API with k6.

**Architecture:** Keep Phase 6 experiment indexes out of the base schema and create/drop them only from numbered SQL prepare scripts. Use `JdbcTemplate` for one fixed measurement endpoint so the API SQL shape stays close to the `psql` evidence query. Store primary evidence under `docs/evidence/phase-06/` and keep phase documentation under the standard five-file phase directory.

**Tech Stack:** Spring Boot 4, Java records, JdbcTemplate, PostgreSQL docker compose, `psql`, `EXPLAIN (ANALYZE, BUFFERS)`, `pg_stat_statements`, k6, Grafana, JUnit Jupiter, AssertJ, Testcontainers.

---

## Spec

Implement [Phase 6 Aggregation Design](../../specs/2026-05-26-phase-6-aggregation-design.md) and keep [Phase 6 roadmap](../../../roadmap/07-phase-6-aggregation.md) aligned.

## Plan Decisions

| Question | Plan Decision |
|---|---|
| Primary evidence? | SQL-only `EXPLAIN (ANALYZE, BUFFERS)` captured through `psql`. |
| SQL script shape? | Numbered files under `scripts/phase-06/`, split into `prepare` and `explain`. |
| Experiment indexes? | Created and dropped only in Phase 6 prepare scripts. Do not modify `docker/postgres/init.sql`. |
| Product Review Summary API? | Add fixed measurement endpoint `GET /api/products/review-summary`. |
| API query implementation? | Use `JdbcTemplate` native SQL, not QueryDSL. |
| API parameters? | None in Phase 6. Query uses fixed `HAVING COUNT(r.id) >= 10` and `LIMIT 100`. |
| k6 scope? | Run only review summary API under `naive index` and `query-shaped index`. |
| Baseline k6? | Do not run. Baseline is SQL-only evidence. |
| Grafana scope? | Reuse the shared `DB Lab Overview` dashboard and add only a `Phase 6 Aggregation Focus` row plus `phase-06` capture mapping. |
| Command interface? | Introduce the root `Makefile` as the public execution interface. Keep implementation in `scripts/` and `package.json` scripts. |
| Phase 7 scope? | Excluded. Pagination remains Phase 7. |

## Vertical Slices

Run slices in order. Each slice leaves the repository in a reviewable state.

| Slice | Document | Outcome |
|---|---|---|
| 001 | [Phase Documentation Scaffold](./001-phase-documentation-scaffold.md) | Phase 6 phase docs and evidence index exist with agreed scope and runbook |
| 002 | [Product Review Summary API](./002-product-review-summary-api.md) | `GET /api/products/review-summary` returns deterministic top 100 review summary rows |
| 003 | [SQL Scripts And Data Profile](./003-sql-scripts-and-data-profile.md) | Numbered SQL scripts reproduce data profile and six SQL-only EXPLAIN conditions |
| 004 | [k6 Review Summary Scenario](./004-k6-review-summary-scenario.md) | k6 scenario and preset can exercise the review summary API |
| 005 | [Grafana Phase 6 Focus Row](./005-grafana-phase6-focus-row.md) | Shared Grafana dashboard and capture tooling support `phase-06` |
| 006 | [Standard Command Interface](./006-standard-command-interface.md) | Root `Makefile` exposes common commands for SQL, k6, Grafana, and evidence capture |
| 007 | [Evidence Capture And Report](./007-evidence-capture-and-report.md) | Phase 6 evidence files, evidence index, and report are populated through standard commands |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | Focused tests, compile checks, docs consistency, and evidence structure checks pass |

## File Ownership

| Path | Responsibility |
|---|---|
| `ecommerce/src/main/java/com/dblab/ecommerce/dto/ProductReviewSummaryResponse.java` | API response DTO for review summary rows |
| `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductReviewSummaryRepository.java` | JdbcTemplate native SQL query for the measurement endpoint |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java` | Service method delegating to the review summary repository |
| `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java` | Exposes `GET /api/products/review-summary` |
| `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductReviewSummaryTest.java` | API/repository behavior tests for summary rows |
| `ecommerce/src/test/resources/test-data/phase6-review-summary-setup.sql` | Minimal deterministic fixture for review summary tests |
| `scripts/phase-06/*.sql` | Reproducible data profile, prepare, and explain scripts |
| `scripts/run-phase-sql.mjs` | Maps `PHASE`, `SCENARIO`, `CONDITION`, and `ACTION` to numbered SQL files and optional output files |
| `scripts/generate-db-lab-dashboard.mjs` | Adds `Phase 6 Aggregation Focus` to the shared dashboard |
| `scripts/grafana-capture-utils.mjs` | Maps `phase-06` to the Phase 6 focus row for screenshot capture |
| `scripts/grafana-capture-utils.test.mjs` | Regression tests for Phase 6 Grafana capture mapping |
| `docker/grafana/dashboards/db-lab-overview.json` | Generated shared Grafana dashboard JSON |
| `Makefile` | Public command interface for repeatable project tasks |
| `k6/review-summary-test.js` | k6 scenario for the review summary endpoint |
| `k6/presets/review-summary-baseline.json` | k6 preset for representative API evidence |
| `docs/phases/06-aggregation/` | Standard Phase 6 phase documentation |
| `docs/evidence/phase-06/` | Captured Phase 6 evidence and summaries |
| `docs/guides/commands.md` | User-facing Makefile command reference |
| `docs/guides/scripts.md` | Internal script reference updated to point users at Makefile for repeated tasks |
| `docs/guides/grafana-observability.md` | Documents the Phase 6 shared dashboard focus row |
| `docs/guides/k6-load-testing.md` | Documents the new k6 scenario |

## Cross-Slice Invariants

- Do not modify `docker/postgres/init.sql` for Phase 6 custom indexes.
- Keep all Phase 6 custom index DDL in `scripts/phase-06/*-prepare.sql`.
- Keep `GET /api/products/review-summary` parameterless.
- Keep Product Review Summary ordering deterministic: `AVG(r.rating) DESC, productId ASC`.
- Keep SQL-only baseline out of k6.
- Use `EXPLAIN (ANALYZE, BUFFERS)` for SQL-only evidence.
- Use `VACUUM (ANALYZE)` and `pg_stat_statements_reset()` in prepare scripts.
- Keep Phase 6 docs directory to exactly the standard five files.
- Do not create a separate Grafana dashboard JSON for Phase 6.
- Keep Makefile targets thin. Do not put long SQL, query text, or evidence orchestration logic directly in `Makefile`.
- Prefer `make ... PHASE=phase-06 SCENARIO=... CONDITION=...` in runbooks and evidence instructions.

## Verification Policy

Run focused checks from `ecommerce/` after code slices:

```bash
rtk gradlew test --tests "*ProductReviewSummaryTest"
rtk gradlew compileJava
```

Run docs and script checks from the repository root:

```bash
rtk rg -n "GET /api/products/review-summary|Product Review Summary|phase-06|VACUUM \\(ANALYZE\\)|pg_stat_statements_reset" docs scripts k6 ecommerce/src/main ecommerce/src/test
rtk powershell -NoProfile -Command "$paths = @('docs/phases/06-aggregation/README.md','docs/phases/06-aggregation/scope.md','docs/phases/06-aggregation/runbook.md','docs/phases/06-aggregation/observability.md','docs/phases/06-aggregation/report.md','docs/evidence/phase-06/README.md','scripts/phase-06/00-data-profile.sql','k6/review-summary-test.js'); $paths | ForEach-Object { if (-not (Test-Path $_)) { throw \"Missing $_\" } }"
rtk make help
rtk node --test scripts/grafana-capture-utils.test.mjs
rtk node scripts/verify-observability.mjs
```
