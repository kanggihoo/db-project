# 999 Integration Stabilization

## Goal

Run final checks after Phase 6 implementation and evidence capture.

## Files

- Verify: `docs/roadmap/07-phase-6-aggregation.md`
- Verify: `docs/superpowers/specs/2026-05-26-phase-6-aggregation-design.md`
- Verify: `docs/superpowers/plans/phase-06-aggregation/`
- Verify: `docs/phases/06-aggregation/`
- Verify: `docs/evidence/phase-06/`
- Verify: `scripts/phase-06/`
- Verify: `scripts/run-phase-sql.mjs`
- Verify: `Makefile`
- Verify: `docs/guides/commands.md`
- Verify: `docker/grafana/dashboards/db-lab-overview.json`
- Verify: `k6/`
- Verify: `ecommerce/`

## Steps

- [ ] **Step 1: Run focused API test**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*ProductReviewSummaryTest"
```

Expected: Gradle exits 0.

- [ ] **Step 2: Run existing product strategy regression test**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyTest"
```

Expected: Gradle exits 0.

- [ ] **Step 3: Run compile check**

Run:

```bash
cd ecommerce && rtk gradlew compileJava
```

Expected: Gradle exits 0.

- [ ] **Step 4: Verify phase doc file count**

Run:

```bash
rtk powershell -NoProfile -Command "$files = Get-ChildItem -LiteralPath 'docs/phases/06-aggregation' -File | Select-Object -ExpandProperty Name; $expected = @('README.md','scope.md','runbook.md','observability.md','report.md'); Compare-Object $expected $files | ForEach-Object { throw \"Unexpected phase file set: $($_.InputObject)\" }"
```

Expected: command exits 0.

- [ ] **Step 5: Verify required SQL scripts exist**

Run:

```bash
rtk powershell -NoProfile -Command "$paths = @('scripts/phase-06/00-data-profile.sql','scripts/phase-06/10-review-baseline-prepare.sql','scripts/phase-06/11-review-baseline-explain.sql','scripts/phase-06/12-review-naive-prepare.sql','scripts/phase-06/13-review-naive-explain.sql','scripts/phase-06/14-review-query-shaped-prepare.sql','scripts/phase-06/15-review-query-shaped-explain.sql','scripts/phase-06/20-monthly-baseline-prepare.sql','scripts/phase-06/21-monthly-baseline-explain.sql','scripts/phase-06/22-monthly-naive-prepare.sql','scripts/phase-06/23-monthly-naive-explain.sql','scripts/phase-06/24-monthly-query-shaped-prepare.sql','scripts/phase-06/25-monthly-query-shaped-explain.sql'); $paths | ForEach-Object { if (-not (Test-Path $_)) { throw \"Missing $_\" } }"
```

Expected: command exits 0.

- [ ] **Step 6: Verify required evidence files exist**

Run:

```bash
rtk powershell -NoProfile -Command "$paths = @('docs/evidence/phase-06/README.md','docs/evidence/phase-06/data-profile/row-counts.txt','docs/evidence/phase-06/review-aggregate/baseline/explain.txt','docs/evidence/phase-06/review-aggregate/naive-index/explain.txt','docs/evidence/phase-06/review-aggregate/query-shaped-index/explain.txt','docs/evidence/phase-06/monthly-order-aggregate/baseline/explain.txt','docs/evidence/phase-06/monthly-order-aggregate/naive-index/explain.txt','docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt','docs/evidence/phase-06/review-summary-api/naive-index/k6-summary.txt','docs/evidence/phase-06/review-summary-api/naive-index/run-window.json','docs/evidence/phase-06/review-summary-api/query-shaped-index/k6-summary.txt','docs/evidence/phase-06/review-summary-api/query-shaped-index/run-window.json','docs/evidence/phase-06/grafana-screenshots/review-summary-naive-index.png','docs/evidence/phase-06/grafana-screenshots/review-summary-query-shaped-index.png'); $paths | ForEach-Object { if (-not (Test-Path $_)) { throw \"Missing $_\" } }"
```

Expected: command exits 0.

- [ ] **Step 7: Verify standard command interface**

Run:

```bash
rtk make help
rtk rg -n "phase-sql|k6-evidence|grafana-capture|evidence-capture|Phase 6 Examples" Makefile docs/guides/commands.md docs/phases/06-aggregation/runbook.md docs/phases/06-aggregation/observability.md
```

Expected: command exits 0 and output shows standard Makefile targets and Phase 6 examples.

- [ ] **Step 8: Verify Grafana Phase 6 focus support**

Run:

```bash
rtk node --test scripts/grafana-capture-utils.test.mjs
rtk node scripts/verify-observability.mjs
rtk rg -n "Phase 6 Aggregation Focus|phase-06" scripts/generate-db-lab-dashboard.mjs scripts/grafana-capture-utils.mjs scripts/grafana-capture-utils.test.mjs docker/grafana/dashboards/db-lab-overview.json docs/guides
```

Expected: tests pass and output includes capture mapping, generated dashboard, and docs references.

- [ ] **Step 9: Verify no Phase 6 custom index was added to base schema**

Run:

```bash
rtk rg -n "idx_review_product_id|idx_review_product_rating|idx_orders_created_at|idx_orders_month_user" docker/postgres/init.sql ecommerce/src/test/resources/init.sql
```

Expected: command exits 1 with no matches.

- [ ] **Step 10: Verify Phase 6 references are aligned**

Run:

```bash
rtk rg -n "Phase 6|phase-06|06-aggregation|Product Review Summary|review-summary|DATE_TRUNC|VACUUM \\(ANALYZE\\)|pg_stat_statements_reset|make phase-sql|Phase 6 Aggregation Focus" docs/roadmap/07-phase-6-aggregation.md docs/superpowers/specs/2026-05-26-phase-6-aggregation-design.md docs/superpowers/plans/phase-06-aggregation docs/phases/06-aggregation docs/evidence/phase-06 scripts/phase-06 scripts/generate-db-lab-dashboard.mjs scripts/grafana-capture-utils.mjs Makefile k6
```

Expected: output includes roadmap, spec, plan, phase docs, evidence, scripts, and k6 references.

- [ ] **Step 11: Check working tree before handoff**

Run:

```bash
rtk git status --short --branch
```

Expected: current branch is the Phase 6 branch and working tree is clean after the final commit.
