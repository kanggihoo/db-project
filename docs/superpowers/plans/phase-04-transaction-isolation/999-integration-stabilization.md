# 999 Integration Stabilization

## Goal

Verify the complete Phase 4 implementation, evidence, docs, and plan consistency before handing the work off.

## Files

- Read: `docs/superpowers/specs/2026-05-22-phase-4-transaction-isolation-design.md`
- Read: `docs/superpowers/plans/phase-04-transaction-isolation/000-plan-index.md`
- Read: `docs/roadmap/05-phase-4-transaction-isolation.md`
- Read: `docs/phases/04-transaction-isolation/*.md`
- Read: `docs/evidence/phase-04/README.md`
- Verify: `ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java`

## Steps

- [ ] **Step 1: Run focused Phase 4 tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest"
```

Expected: Gradle exits 0.

- [ ] **Step 2: Run the broader test suite**

Run:

```bash
cd ecommerce && rtk gradlew test
```

Expected: Gradle exits 0.

- [ ] **Step 3: Verify no forbidden Phase 4 strategy implementation was added**

Run:

```bash
rtk rg -n "PESSIMISTIC|OPTIMISTIC|FOR UPDATE|Atomic UPDATE|Idempotency|idempotency|retry" ecommerce/src/test/java/com/dblab/ecommerce/isolation docs/phases/04-transaction-isolation docs/evidence/phase-04
```

Expected: matches only appear in documentation text that explicitly says these strategies are Phase 11 scope. No `TransactionIsolationTest` code implements these strategies.

- [ ] **Step 4: Verify evidence exists**

Run:

```bash
rtk proxy powershell -NoProfile -Command "Test-Path 'docs/evidence/phase-04/dirty-read/integration-test-output.txt'; Test-Path 'docs/evidence/phase-04/non-repeatable-read/integration-test-output.txt'; Test-Path 'docs/evidence/phase-04/phantom-read/integration-test-output.txt'; Test-Path 'docs/evidence/phase-04/lost-update/integration-test-output.txt'"
```

Expected: four `True` lines.

- [ ] **Step 5: Verify docs mention all four phenomena**

Run:

```bash
rtk rg -n "Dirty Read|Non-Repeatable Read|Phantom Read|Lost Update" docs/roadmap/05-phase-4-transaction-isolation.md docs/phases/04-transaction-isolation docs/evidence/phase-04
```

Expected: each phenomenon appears in roadmap, phase docs, and evidence.

- [ ] **Step 6: Verify spec acceptance criteria are covered**

Read:

```bash
rtk proxy powershell -NoProfile -Command "Get-Content -Raw -Encoding UTF8 'docs/superpowers/specs/2026-05-22-phase-4-transaction-isolation-design.md'"
rtk proxy powershell -NoProfile -Command "Get-Content -Raw -Encoding UTF8 'docs/phases/04-transaction-isolation/report.md'"
```

Expected: every acceptance criterion in the spec maps to a passing test, evidence file, or report statement.

- [ ] **Step 7: Verify git status**

Run:

```bash
rtk git status --short
```

Expected: only intentional files are modified before final commit, or working tree is clean after final commit.

- [ ] **Step 8: Final commit if stabilization changed files**

If Steps 1-7 required code or documentation changes, commit them:

```bash
git add ecommerce/src/test/java/com/dblab/ecommerce/isolation/TransactionIsolationTest.java docs/evidence/phase-04 docs/phases/04-transaction-isolation docs/roadmap/05-phase-4-transaction-isolation.md
git commit -m "chore: stabilize phase 4 transaction isolation"
```

If no files changed, do not create an empty commit.
