# 003 Preset Variable Compatibility

## Goal

`PRESET` 기존 사용 방식을 유지하면서 `K6_PRESET`과 `GRAFANA_PRESET` override가 의도대로 동작하는지 검증한다.

## Files

- Inspect: `makefiles/config.mk`
- Inspect: `makefiles/k6.mk`
- Inspect: `makefiles/grafana.mk`
- Inspect: `makefiles/evidence.mk`

## Steps

- [ ] **Step 1: Verify default preset inheritance in k6**

Run:

```bash
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index
```

Expected command shape:

```text
npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --mode prometheus --condition naive-index
```

- [ ] **Step 2: Verify explicit `K6_PRESET` override**

Run:

```bash
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=ignored K6_PRESET=review-summary-baseline CONDITION=naive-index
```

Expected command shape:

```text
npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --mode prometheus --condition naive-index
```

The command must not pass `--preset ignored`.

- [ ] **Step 3: Verify default preset inheritance in Grafana capture**

Run:

```bash
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index TABLE=review
```

Expected command shape:

```text
npm run grafana:capture -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --table review --no-align-phase-rows
```

- [ ] **Step 4: Verify explicit `GRAFANA_PRESET` override**

Run:

```bash
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=ignored GRAFANA_PRESET=baseline CONDITION=naive-index TABLE=review
```

Expected command shape:

```text
npm run grafana:capture -- --phase phase-06 --scenario review-summary --preset baseline --pool pool10 --table review --no-align-phase-rows
```

The command must not pass `--preset ignored`.

- [ ] **Step 5: Verify `evidence-capture` uses `K6_PRESET`**

Run:

```bash
rtk proxy make -n evidence-capture PHASE=phase-06 SCENARIO=review-summary PRESET=ignored K6_PRESET=review-summary-baseline CONDITION=query-shaped-index TABLE=review
```

Expected command shape:

```text
npm run evidence:capture -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --mode prometheus --condition query-shaped-index --table review
```

- [ ] **Step 6: Keep the current `evidence-capture` limitation explicit**

Confirm the implementation still follows the spec:

```text
evidence-capture passes one --preset because scripts/run-k6-evidence.mjs owns the combined k6 + capture flow. Makefile exposes GRAFANA_PRESET for direct grafana-capture and keeps evidence-capture on K6_PRESET until the Node wrapper supports split preset values.
```

- [ ] **Step 7: Commit**

If this slice changes files beyond slice 002:

```bash
git add Makefile makefiles
git commit -m "refactor(make): separate k6 and grafana preset variables"
```

If no files changed because slice 002 already implemented this behavior, do not commit.
