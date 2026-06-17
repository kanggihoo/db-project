# 999 Integration Stabilization

## Goal

모든 k6 scenario 리팩토링 후 전체 실행 인터페이스와 evidence label 계약이 유지되는지 최종 점검한다.

## Files

- Inspect: `k6/lib/*.js`
- Inspect: `k6/*-test.js`
- Inspect: `k6/run.sh`
- Inspect: `scripts/run-k6-evidence.mjs`
- Inspect: `Makefile`
- Inspect: `docs/guides/k6-load-testing.md`

## Steps

- [ ] **Step 1: Confirm no entrypoint was removed**

Run:

```bash
rtk proxy sh -lc 'find k6 -maxdepth 1 -name "*-test.js" | sort'
```

Expected: existing scenario entrypoint files are still present.

- [ ] **Step 2: Confirm `run.sh` contract is unchanged**

Run:

```bash
rtk rg -n "PRESET_NAME|PHASE|SCENARIO|POOL|K6_RUN_WINDOW_FILE|run-window" k6/run.sh scripts/run-k6-evidence.mjs Makefile
```

Expected:

- `k6/run.sh <scenario> <preset> <mode>` still works.
- `PRESET` remains the preset file path passed to k6.
- `PRESET_NAME` remains the label value.
- run-window metadata still records `phase`, `scenario`, `preset`, `pool`.

- [ ] **Step 3: Confirm k6 lib boundary**

Run:

```bash
rtk rg -n "baseUrl|timeout|thresholds|systemTags|constant-arrival-rate|check\\(" k6/lib k6/*-test.js
```

Expected:

- common base URL, timeout, thresholds, options, and common checks live in `k6/lib`.
- scenario files still show their URL and parameter selection.

- [ ] **Step 4: Run static verification**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
rtk rg -n "node:|require\\(|process\\.|fs\\.|path\\." k6/lib
```

Expected: verifier exits 0 and no Node-only API is used in k6 lib.

- [ ] **Step 5: Run representative k6 smoke checks when available**

Run:

```bash
rtk npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --condition refactor-smoke
rtk npm run k6:evidence -- --phase phase-07 --scenario points --preset points-page0 --condition refactor-smoke
```

If services are unavailable, do not fake success. Record the blocker and rely on static verification for this branch.

- [ ] **Step 6: Review diff for behavior drift**

Run:

```bash
rtk git diff
```

Check for unintended changes:

- changed preset values
- changed threshold values
- changed endpoint paths
- changed label names
- changed scenario names
- added high-cardinality labels

- [ ] **Step 7: Commit final stabilization changes**

```bash
git add k6 docs scripts Makefile
git commit -m "test(k6): stabilize shared lib refactor"
```
