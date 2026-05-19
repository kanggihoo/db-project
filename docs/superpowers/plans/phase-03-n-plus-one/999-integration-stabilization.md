# 999 Integration Stabilization

## Goal

Verify the complete Phase 3 implementation, documentation, and evidence links before handing the branch off.

## Files

- Read: `docs/superpowers/specs/phase-3-n-plus-one-spec.md`
- Read: `docs/superpowers/plans/phase-03-n-plus-one/index.md`
- Read: `docs/roadmap/04-phase-3-n-plus-one.md`
- Read: `docs/phases/03-n-plus-one/*.md`
- Read: `docs/evidence/phase-03/README.md`
- Verify: application tests, generated dashboard, k6 script labels

## Steps

- [ ] **Step 1: Run application tests**

Run:

```bash
cd ecommerce && rtk gradlew test
```

Expected: Gradle test task exits 0.

- [ ] **Step 2: Regenerate dashboard**

Run:

```bash
rtk npm run grafana:generate
```

Expected: command exits 0 and `docker/grafana/dashboards/db-lab-overview.json` contains `strategy`.

- [ ] **Step 3: Verify strategy wiring**

Run:

```bash
rtk grep "strategy" k6/orders-test.js k6/run.sh scripts/generate-db-lab-dashboard.mjs
rtk grep "OrderLoadingStrategy" ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java
```

Expected: both grep commands return matches.

- [ ] **Step 4: Verify Phase 3 docs exist**

Run:

```bash
rtk proxy find docs/phases/03-n-plus-one -maxdepth 1 -type f | sort
rtk proxy find docs/superpowers/plans/phase-03-n-plus-one -maxdepth 1 -type f | sort
```

Expected:

- Phase docs include `README.md`, `scope.md`, `runbook.md`, `observability.md`, `report.md`.
- Plan docs include `index.md`, numbered slices, and `999-integration-stabilization.md`.

- [ ] **Step 5: Verify spec coverage**

Read the spec and confirm each acceptance criterion maps to code, docs, or evidence:

```bash
rtk read docs/superpowers/specs/phase-3-n-plus-one-spec.md
rtk read docs/phases/03-n-plus-one/report.md
```

Expected: no acceptance criterion remains unaddressed. If a criterion is intentionally deferred, `report.md` states the reason.

- [ ] **Step 6: Verify git status**

Run:

```bash
rtk git status
```

Expected: clean working tree after all intended commits.

- [ ] **Step 7: Final commit if stabilization changed files**

If Step 1-6 required doc or code changes, commit them:

```bash
git add <changed-files>
git commit -m "chore: stabilize phase 3 n plus one implementation"
```

If no files changed, do not create an empty commit.
