# 007 Evidence Capture And Report

## Goal

Capture Phase 6 evidence from the implemented SQL scripts and review summary API through the standard Makefile interface, then update the evidence index and phase report.

## Files

- Create directories under: `docs/evidence/phase-06/`
- Modify: `docs/evidence/phase-06/README.md`
- Modify: `docs/phases/06-aggregation/report.md`
- Modify: `docs/phases/06-aggregation/README.md`
- Modify: `docs/phases/06-aggregation/scope.md`

## Steps

- [ ] **Step 1: Create evidence directories**

Run:

```bash
rtk proxy mkdir -p \
  docs/evidence/phase-06/data-profile \
  docs/evidence/phase-06/review-aggregate/baseline \
  docs/evidence/phase-06/review-aggregate/naive-index \
  docs/evidence/phase-06/review-aggregate/query-shaped-index \
  docs/evidence/phase-06/monthly-order-aggregate/baseline \
  docs/evidence/phase-06/monthly-order-aggregate/naive-index \
  docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index \
  docs/evidence/phase-06/review-summary-api/naive-index \
  docs/evidence/phase-06/review-summary-api/query-shaped-index \
  docs/evidence/phase-06/grafana-screenshots
```

Expected: command exits 0.

- [ ] **Step 2: Capture data profile evidence**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=data-profile ACTION=profile OUTPUT=docs/evidence/phase-06/data-profile/row-counts.txt
```

Expected: `row-counts.txt` contains `PHASE6_ROW_COUNTS`, `PHASE6_REVIEWED_PRODUCT_COUNT`, `PHASE6_REVIEW_COUNT_DISTRIBUTION`, `PHASE6_REVIEW_TOP_20_PRODUCTS`, `PHASE6_MONTHLY_ORDER_DISTRIBUTION`, and `PHASE6_INDEX_STATE_BEFORE`.

- [ ] **Step 3: Capture review baseline EXPLAIN**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=baseline ACTION=prepare
rtk make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=baseline ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/baseline/explain.txt
```

Expected: explain file contains `Execution Time`.

- [ ] **Step 4: Capture review naive EXPLAIN**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
rtk make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/naive-index/explain.txt
```

Expected: explain file contains `Execution Time`.

- [ ] **Step 5: Capture review query-shaped EXPLAIN**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=query-shaped-index ACTION=prepare
rtk make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=query-shaped-index ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/query-shaped-index/explain.txt
```

Expected: explain file contains `Execution Time`.

- [ ] **Step 6: Capture monthly baseline EXPLAIN**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=baseline ACTION=prepare
rtk make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=baseline ACTION=explain OUTPUT=docs/evidence/phase-06/monthly-order-aggregate/baseline/explain.txt
```

Expected: explain file contains `Execution Time`.

- [ ] **Step 7: Capture monthly naive EXPLAIN**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=naive-index ACTION=prepare
rtk make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=naive-index ACTION=explain OUTPUT=docs/evidence/phase-06/monthly-order-aggregate/naive-index/explain.txt
```

Expected: explain file contains `Execution Time`.

- [ ] **Step 8: Capture monthly query-shaped EXPLAIN**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=query-shaped-index ACTION=prepare
rtk make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=query-shaped-index ACTION=explain OUTPUT=docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt
```

Expected: explain file contains `Execution Time` or the command fails because PostgreSQL rejected the expression index. If PostgreSQL rejects the expression index, save the error output to `docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt` and explain the failure in `docs/phases/06-aggregation/report.md`.

- [ ] **Step 9: Capture k6 naive API evidence**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
rtk make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index PRESET=review-summary-baseline POOL=pool10 MODE=prometheus
```

Expected: k6 exits 0 and writes `docs/evidence/phase-06/review-summary-api/naive-index/k6-summary.txt` plus `run-window.json`.

- [ ] **Step 10: Capture k6 query-shaped API evidence**

Run:

```bash
rtk make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=query-shaped-index ACTION=prepare
rtk make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index PRESET=review-summary-baseline POOL=pool10 MODE=prometheus
```

Expected: k6 exits 0 and writes `docs/evidence/phase-06/review-summary-api/query-shaped-index/k6-summary.txt` plus `run-window.json`.

- [ ] **Step 11: Capture Grafana screenshots for API evidence**

Run:

```bash
rtk make grafana-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index PRESET=review-summary-baseline POOL=pool10 TABLE=review WINDOW_FILE=docs/evidence/phase-06/review-summary-api/naive-index/run-window.json OUTPUT=docs/evidence/phase-06/grafana-screenshots/review-summary-naive-index.png
rtk make grafana-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index PRESET=review-summary-baseline POOL=pool10 TABLE=review WINDOW_FILE=docs/evidence/phase-06/review-summary-api/query-shaped-index/run-window.json OUTPUT=docs/evidence/phase-06/grafana-screenshots/review-summary-query-shaped-index.png
```

Expected: screenshots exist and show the common dashboard rows for the fixed k6 run windows. Phase 6 does not use a dedicated Grafana focus row.

- [ ] **Step 12: Update report with measured values**

Modify `docs/phases/06-aggregation/report.md`:

```markdown
## Conclusion

Phase 6 compared Product Review Summary and Monthly Order Aggregate under baseline, naive index, and query-shaped index conditions. Each condition has `EXPLAIN (ANALYZE, BUFFERS)` evidence under `docs/evidence/phase-06/`.

## Interpretation Criteria

- `Execution Time` is database-side query execution time.
- k6 p95 and p99 are HTTP response times for the Product Review Summary API.
- Do not conclude from the aggregate node name alone. Interpret aggregate type together with buffers, sort behavior, scan method, and execution time.
```

Then fill each existing report table with the measured plan summary and execution time from the evidence files.

- [ ] **Step 13: Mark completed scope checklist items**

Modify `docs/phases/06-aggregation/scope.md` so completed items use `[x]` only after the corresponding evidence files exist.

- [ ] **Step 14: Update Phase 6 README status**

Modify `docs/phases/06-aggregation/README.md`:

```markdown
## Current Status

Phase 6 is complete.
```

Add a short bullet list naming the two SQL-only experiments and the Product Review Summary API k6 comparison.

- [ ] **Step 15: Verify evidence markers**

Run:

```bash
rtk rg -n "Execution Time|HashAggregate|GroupAggregate|Seq Scan|Index Scan|Bitmap|Sort|http_req_duration|PHASE6_ROW_COUNTS|review-summary-naive-index|review-summary-query-shaped-index" docs/evidence/phase-06 docs/phases/06-aggregation/report.md
```

Expected: output includes SQL plan markers, k6 marker, data profile marker, and Grafana screenshot references.

- [ ] **Step 16: Commit**

```bash
git add docs/evidence/phase-06 docs/phases/06-aggregation
git commit -m "docs(phase6): capture aggregation evidence report"
```
