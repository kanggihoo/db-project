# Phase 2 Index Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce Phase 2 documentation, SQL scripts, and evidence workflow for comparing the Phase 1 product search baseline with a `product(category_id, status)` index.

**Architecture:** Keep the Phase 1 product API path unchanged and treat PostgreSQL indexes as the experimental variable. Use manual SQL scripts for repeatable create/drop/explain operations, k6/Grafana/`pg_stat_statements` for the main API comparison, and separate SQL-only experiments for index concepts that do not map directly to the current API.

**Tech Stack:** PostgreSQL 17, `psql`, Spring Boot, k6, Prometheus, Grafana, Markdown phase documentation.

---

## Spec

Implement [Phase 2 Index Optimization Spec](../../specs/phase-2-index-optimization-spec.md) and keep [Phase 2 roadmap](../../../roadmap/03-phase-2-indexes.md) aligned.

## Plan Decisions

The spec open questions are resolved for this plan:

| Question | Plan Decision |
|---|---|
| Manual SQL only or rollback helper? | Use manual SQL files plus an explicit cleanup SQL file. |
| Consolidated or per-topic SQL-only scripts? | Use separate SQL files per experiment topic. |
| Baseline only or stress-100? | Require `products baseline`; leave `stress-100` as an optional stabilization note, not an acceptance criterion. |

## Vertical Slices

Run the slices in order. Each slice leaves the repository or evidence tree in a reviewable state.

| Slice | Document | Outcome |
|---|---|---|
| 001 | [Phase Documentation Scaffold](./001-phase-documentation-scaffold.md) | `docs/phases/02-indexes/` and `docs/evidence/phase-02/` are ready |
| 002 | [SQL Script Kit](./002-sql-script-kit.md) | Repeatable index cleanup, create, explain, and snapshot SQL scripts exist |
| 003 | [Main Product Index Experiment](./003-main-product-index-experiment.md) | Main pre/post index evidence is captured using the existing product API |
| 004 | [SQL-only Auxiliary Experiments](./004-sql-only-auxiliary-experiments.md) | Single-column, composite order, covering, and partial index concepts are captured separately |
| 005 | [Report And Phase Closeout](./005-report-and-phase-closeout.md) | Phase report compares results and records the Phase 3 handoff |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | Final consistency checks pass and docs/evidence links resolve |

## File Ownership

| Path | Responsibility |
|---|---|
| `docs/phases/02-indexes/README.md` | Phase 2 hub, status, source docs, evidence links |
| `docs/phases/02-indexes/scope.md` | Phase 2 goals, non-goals, target API/table, completion criteria |
| `docs/phases/02-indexes/runbook.md` | Repeatable commands for SQL, k6, `pg_stat_statements`, and Grafana capture |
| `docs/phases/02-indexes/observability.md` | How to interpret `EXPLAIN`, `pg_stat_statements`, k6, and Grafana |
| `docs/phases/02-indexes/report.md` | Final measured comparison, SQL-only insights, Phase 3 handoff |
| `docs/evidence/phase-02/README.md` | Evidence index for Phase 2 raw outputs |
| `scripts/phase-02/00-clean-product-indexes.sql` | Removes experimental product indexes before isolated runs |
| `scripts/phase-02/01-main-pre-index-explain.sql` | Captures representative product query plan before the main index |
| `scripts/phase-02/02-create-main-index.sql` | Creates `idx_product_category_status` |
| `scripts/phase-02/03-main-post-index-explain.sql` | Captures representative product query plan after the main index |
| `scripts/phase-02/04-product-pg-stat-statements.sql` | Captures product query `pg_stat_statements` snapshot |
| `scripts/phase-02/10-single-status-index.sql` | SQL-only single-column selectivity experiment |
| `scripts/phase-02/20-composite-order-index.sql` | SQL-only composite index order experiment |
| `scripts/phase-02/30-covering-index.sql` | SQL-only covering index experiment |
| `scripts/phase-02/40-partial-index.sql` | SQL-only partial index experiment |
| `docs/superpowers/README.md` | Links this plan index |

## Cross-Slice Invariants

- Main API path stays `GET /api/products?categoryId=&status=`.
- Main repository method stays `findByCategoryIdAndStatus`.
- Main index name is `idx_product_category_status`.
- Main index definition is `ON product(category_id, status)`.
- Main post-index k6 command uses `PHASE=phase-02 POOL=pool10 ./k6/run.sh products baseline prometheus`.
- SQL-only evidence is not compared directly with k6/API latency evidence.
- Evidence is stored under `docs/evidence/phase-02/`.

## Verification Policy

Every slice must run its listed verification commands. Documentation-only slices verify file presence and link consistency. Experiment slices verify that output files exist and contain expected PostgreSQL plan or k6 markers.
