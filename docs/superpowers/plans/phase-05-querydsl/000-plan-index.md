# Phase 5 QueryDSL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phase 5 by comparing baseline entity-based product search with QueryDSL DTO projection through a strategy parameter, then capture SQL/test evidence.

**Architecture:** Keep `/api/products` as the comparison surface and add `strategy=baseline|querydsl` without changing the `ProductResponse` shape. The baseline path preserves the existing Spring Data JPA entity query, while the QueryDSL path uses a focused custom repository to project directly into `ProductResponse`. Evidence comes from Testcontainers integration tests, Hibernate statistics, representative SQL logs, and documentation under `docs/evidence/phase-05/`.

**Tech Stack:** Spring Boot 4, Spring Data JPA, QueryDSL 5.1 Jakarta, PostgreSQL Testcontainers, JUnit Jupiter, AssertJ, Hibernate statistics.

---

## Spec

Implement [Phase 5 QueryDSL Design](../../specs/2026-05-26-phase-5-querydsl-design.md) and keep [Phase 5 roadmap](../../../roadmap/06-phase-5-querydsl.md) aligned.

## Plan Decisions

| Question | Plan Decision |
|---|---|
| Product API comparison surface? | Use existing `GET /api/products`. |
| Strategy parameter? | Add optional `strategy` request parameter. |
| Default strategy? | `querydsl`. |
| Baseline behavior? | Existing entity query then `ProductResponse.from(product)`. |
| QueryDSL behavior? | DTO projection selecting only `ProductResponse` fields. |
| Dynamic conditions? | `categoryId` and `status` are optional and omitted from QueryDSL predicates when null. |
| k6/Grafana? | Not required for Phase 5 completion. |
| Bulk update? | Test/evidence comparison only; no user-facing API. |

## Vertical Slices

Run slices in order. Each slice leaves the repository in a reviewable state.

| Slice | Document | Outcome |
|---|---|---|
| 001 | [Phase Documentation Scaffold And API Contract](./001-phase-documentation-and-api-contract.md) | Phase 5 docs exist and product search accepts `strategy` with default `querydsl` |
| 002 | [QueryDSL Product Search Repository](./002-querydsl-product-search-repository.md) | QueryDSL repository returns `ProductResponse` projection and supports optional `categoryId`/`status` |
| 003 | [Product Strategy Tests And SQL Evidence](./003-product-strategy-tests-and-sql-evidence.md) | Baseline/querydsl strategy tests pass and product search evidence files are captured |
| 004 | [Bulk Update Comparison](./004-bulk-update-comparison.md) | Row-by-row order status update and JPQL bulk update are compared with SQL count/stale context tests |
| 005 | [Evidence And Phase Closeout](./005-evidence-and-phase-closeout.md) | Evidence index, Phase 5 report, and Phase 6 handoff are complete |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | Focused tests, docs consistency, and plan self-checks pass |

## File Ownership

| Path | Responsibility |
|---|---|
| `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategy.java` | Strategy enum for product search comparison |
| `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java` | Exposes optional `strategy` parameter on existing product search endpoint |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java` | Routes product search to baseline or QueryDSL path |
| `ecommerce/src/main/java/com/dblab/ecommerce/repository/ProductQueryRepository.java` | QueryDSL DTO projection and optional predicate composition |
| `ecommerce/src/main/java/com/dblab/ecommerce/config/QuerydslConfig.java` | Provides `JPAQueryFactory` bean |
| `ecommerce/src/main/java/com/dblab/ecommerce/entity/Orders.java` | Adds minimal status transition method for row-by-row baseline update |
| `ecommerce/src/main/java/com/dblab/ecommerce/repository/OrderRepository.java` | Adds status lookup and JPQL bulk update methods |
| `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java` | Product search strategy behavior and SQL count tests |
| `ecommerce/src/test/java/com/dblab/ecommerce/order/OrderBulkUpdateTest.java` | Bulk update SQL count and persistence context behavior tests |
| `docs/phases/05-querydsl/` | Standard Phase 5 phase documentation |
| `docs/evidence/phase-05/` | Captured Phase 5 evidence and summaries |

## Cross-Slice Invariants

- Keep the product API response shape as `List<ProductResponse>`.
- Do not remove the baseline product search path.
- Do not add `minPrice`, `maxPrice`, or `keyword` in this implementation.
- Treat `categoryId` and `status` as optional for Phase 5 product search.
- Default omitted product search strategy to `querydsl`.
- Keep k6/Grafana out of the required evidence path.
- Do not implement stock, coupon, retry, lock, or idempotency concurrency strategy work in Phase 5.
- Use PostgreSQL/Testcontainers for database behavior tests.
- Use Hibernate statistics for SQL count assertions when possible.

## Verification Policy

Every code slice must run its listed focused Gradle test command from `ecommerce/`.

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest"
rtk gradlew test --tests "*OrderBulkUpdateTest"
```

Documentation slices must verify that required docs and evidence files exist and contain the expected Phase 5 markers.
