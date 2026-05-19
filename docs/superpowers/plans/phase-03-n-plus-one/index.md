# Phase 3 N+1 Loading Strategy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phase 3 as a comparable N+1 loading-strategy experiment for the order-list thumbnail use case.

**Architecture:** Keep one order-list API and route behavior through `strategy=lazy|fetch-join|batch-size|entity-graph`. Add JPA associations as lazy read paths, keep response shape stable, and compare strategy evidence through SQL count, `pg_stat_statements`, k6, Grafana, and representative EXPLAIN output.

**Tech Stack:** Spring Boot 4, Spring Data JPA, Hibernate, PostgreSQL, k6, Prometheus, Grafana, Markdown phase documentation.

---

## Spec

Implement [Phase 3 N+1 Loading Strategy Spec](../../specs/phase-3-n-plus-one-spec.md) and keep [Phase 3 roadmap](../../../roadmap/04-phase-3-n-plus-one.md) aligned.

## Plan Decisions

| Question | Plan Decision |
|---|---|
| EntityGraph included? | Implement it as a separate comparable strategy after Lazy, Fetch Join, and BatchSize. |
| BatchSize isolation? | Use a dedicated Spring profile `phase3-batch` so Lazy and Fetch Join evidence are not contaminated by global batch fetching. |
| Product image loading? | Use the representative main image: first `ProductImage` where `isMain = true`, falling back to the first sorted image if no main image exists. |
| Grafana strategy variable? | Add a low-cardinality `strategy` k6 label and a `$strategy` dashboard variable before evidence collection. |

## Vertical Slices

Run slices in order. Each slice leaves the repository in a reviewable state.

| Slice | Document | Outcome |
|---|---|---|
| 001 | [Phase Documentation Scaffold](./001-phase-documentation-scaffold.md) | `docs/phases/03-n-plus-one/` and `docs/evidence/phase-03/` are ready |
| 002 | [Entity Associations And Response Contract](./002-entity-associations-and-response-contract.md) | Lazy association path and thumbnail response field exist with focused tests |
| 003 | [Strategy Routing And Lazy Baseline](./003-strategy-routing-and-lazy-baseline.md) | `strategy=lazy` reproduces association-based N+1 |
| 004 | [Fetch Join Strategy](./004-fetch-join-strategy.md) | `strategy=fetch-join` uses repository fetch joins and documents limits |
| 005 | [BatchSize Strategy](./005-batch-size-strategy.md) | `strategy=batch-size` is isolated by profile and produces `IN (...)` query shapes |
| 006 | [EntityGraph Strategy](./006-entity-graph-strategy.md) | `strategy=entity-graph` uses repository-level graph loading |
| 007 | [k6 And Grafana Strategy Observability](./007-k6-and-grafana-strategy-observability.md) | k6 emits `strategy`, dashboard can filter/compare strategies |
| 008 | [Evidence Capture Workflow](./008-evidence-capture-workflow.md) | SQL count, EXPLAIN, k6, `pg_stat_statements`, and screenshots are captured per strategy |
| 009 | [Report And Phase Closeout](./009-report-and-phase-closeout.md) | Final Phase 3 report compares strategies and hands off to Phase 4 |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | Final consistency, tests, links, and docs checks pass |

## File Ownership

| Path | Responsibility |
|---|---|
| `docs/phases/03-n-plus-one/README.md` | Phase hub, status, source docs, strategy and evidence links |
| `docs/phases/03-n-plus-one/scope.md` | Goal, target API/entity path, non-goals, completion criteria |
| `docs/phases/03-n-plus-one/runbook.md` | Repeatable commands for strategy execution and evidence capture |
| `docs/phases/03-n-plus-one/observability.md` | SQL count, `pg_stat_statements`, k6, Grafana, EXPLAIN interpretation |
| `docs/phases/03-n-plus-one/report.md` | Final strategy comparison and Phase 4 handoff |
| `docs/evidence/phase-03/README.md` | Evidence index for Phase 3 raw outputs |
| `ecommerce/src/main/java/com/dblab/ecommerce/entity/*.java` | Lazy association path for order-list thumbnail traversal |
| `ecommerce/src/main/java/com/dblab/ecommerce/dto/OrderResponse.java` | Stable response contract with thumbnail URL |
| `ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java` | `strategy` request parameter |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java` | Strategy dispatch and DTO assembly |
| `ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java` | Lazy, fetch-join, and EntityGraph order queries |
| `ecommerce/src/main/resources/application-phase3-batch.yaml` | Isolated Hibernate batch-size profile |
| `k6/orders-test.js` | Sends `strategy` query parameter and label |
| `scripts/generate-db-lab-dashboard.mjs` | Adds `$strategy` variable and Phase 3 strategy panels |
| `scripts/phase-03/*.sql` | SQL count, representative EXPLAIN, and `pg_stat_statements` capture helpers |

## Cross-Slice Invariants

- Public API remains `GET /api/orders?userId=&strategy=`.
- Default strategy is `lazy` when the parameter is absent.
- Unknown strategy returns HTTP 400 with a clear message.
- Order name, option, price, and quantity come from `OrderItem` snapshot fields.
- Product image comes from `OrderItem.productSku.product.images`.
- Entity fetch type stays `LAZY`; do not set persistent associations to `EAGER`.
- BatchSize evidence is collected only under the `phase3-batch` profile.
- Evidence is stored under `docs/evidence/phase-03/orders/<strategy>/`.
- `pg_stat_statements_reset()` runs before each strategy measurement.

## Verification Policy

Every slice must run its listed verification commands. Code slices run focused Gradle tests. Documentation-only slices verify file presence and link consistency. Evidence slices verify that captured files exist and contain expected markers.
