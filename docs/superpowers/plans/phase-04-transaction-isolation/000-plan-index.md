# Phase 4 Transaction Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phase 4 as a PostgreSQL Testcontainers integration-test suite that reproduces Dirty Read prevention, Non-Repeatable Read, Phantom Read, and Lost Update behavior.

**Architecture:** Use the existing Spring test slice and `TestcontainersConfiguration` only to obtain a real PostgreSQL `DataSource`. Each test opens two JDBC connections, sets isolation levels before transaction work, controls commit/rollback order directly, and resets a minimal Product/Product SKU fixture in `@BeforeEach`.

**Tech Stack:** Spring Boot 4, JUnit Jupiter, AssertJ, Spring `@DataJpaTest`, Testcontainers PostgreSQL, JDBC `Connection`, PostgreSQL MVCC.

---

## Spec

Implement [Phase 4 Transaction Isolation Design](../../specs/2026-05-22-phase-4-transaction-isolation-design.md) and keep [Phase 4 roadmap](../../../roadmap/05-phase-4-transaction-isolation.md), [Phase 4 scope](../../../phases/04-transaction-isolation/scope.md), and [Phase 4 report](../../../phases/04-transaction-isolation/report.md) aligned.

## Plan Decisions

| Question | Plan Decision |
|---|---|
| Primary evidence? | Integration test output, not manual SQL transcript. |
| Database source? | PostgreSQL Testcontainers with `src/test/resources/init.sql`, not docker compose volume. |
| Test level? | `@DataJpaTest` + direct JDBC `DataSource` control. |
| Fixture strategy? | Minimal Product/Product SKU rows reset in `@BeforeEach`. |
| Lost Update scope? | Compare `READ COMMITTED` and `REPEATABLE READ` only. |
| k6/Grafana? | Not required for Phase 4 completion. |

## Vertical Slices

Run slices in order. Each slice leaves the repository in a reviewable state.

| Slice | Document | Outcome |
|---|---|---|
| 001 | [Test Scaffold And Fixture Reset](./001-test-scaffold-and-fixture-reset.md) | `TransactionIsolationTest` exists with Testcontainers-backed `DataSource` and idempotent fixture reset |
| 002 | [Dirty And Non-Repeatable Read Tests](./002-dirty-and-non-repeatable-read-tests.md) | Dirty Read prevention and Non-Repeatable Read behavior are asserted |
| 003 | [Phantom Read Tests](./003-phantom-read-tests.md) | READ COMMITTED phantom and PostgreSQL REPEATABLE READ phantom prevention are asserted |
| 004 | [Lost Update Tests](./004-lost-update-tests.md) | Naive READ COMMITTED Lost Update risk and REPEATABLE READ concurrent update failure are asserted |
| 005 | [Evidence And Report Update](./005-evidence-and-report-update.md) | Phase 4 evidence and report summarize the test outcomes |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | Full focused test, docs, and plan consistency checks pass |

## File Ownership

| Path | Responsibility |
|---|---|
| `ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java` | Phase 4 PostgreSQL isolation behavior tests using direct JDBC connections |
| `docs/evidence/phase-04/dirty-read/integration-test-output.txt` | Human-captured evidence for Dirty Read prevention |
| `docs/evidence/phase-04/non-repeatable-read/integration-test-output.txt` | Human-captured evidence for Non-Repeatable Read tests |
| `docs/evidence/phase-04/phantom-read/integration-test-output.txt` | Human-captured evidence for Phantom Read tests |
| `docs/evidence/phase-04/lost-update/integration-test-output.txt` | Human-captured evidence for Lost Update tests |
| `docs/phases/04-transaction-isolation/report.md` | Final Phase 4 results and Phase 5/Phase 11 handoff |

## Cross-Slice Invariants

- Use PostgreSQL Testcontainers, not docker compose PostgreSQL volume.
- Do not use H2, mock DB, or embedded DB for isolation tests.
- Do not call application services, controllers, or repositories from Phase 4 tests.
- Use direct JDBC `Connection` objects for transaction isolation experiments.
- Set isolation level before transaction work begins.
- Close all JDBC resources with try-with-resources.
- Explicitly commit or rollback every transaction.
- Reset fixture rows in `@BeforeEach`; do not rely on Spring test rollback for committed direct JDBC changes.
- Keep fixture data minimal: `category 900001`, `product 900001`, `product_sku 900001`, and temporary phantom `product 900002`.
- Do not implement Atomic UPDATE, pessimistic lock, optimistic lock, retry, or idempotency strategies in Phase 4.

## Verification Policy

Every slice must run its listed verification commands. Code slices run the focused Gradle test command:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest"
```

Documentation slices verify that evidence/report files exist and contain the expected scenario markers.
