# 005 Dry Run And Verification

## Goal

분리된 Makefile이 기존 명령 계약을 유지하고 preset override가 의도대로 확장되는지 검증한다.

## Files

- Inspect: `Makefile`
- Inspect: `makefiles/*.mk`
- Inspect: `docs/guides/commands.md`
- Inspect: `docs/guides/project-format-standard.md`

## Steps

- [ ] **Step 1: Run help**

Run:

```bash
rtk proxy make help
```

Expected:

- exits 0
- public target list is visible
- preset variable explanation is visible

- [ ] **Step 2: Run environment check**

Run:

```bash
rtk proxy make env-check
```

Expected:

- exits 0 when Docker, Node, npm, and k6 or `grafana/k6` image are available
- if this fails because local tooling is unavailable, record the missing tool and continue with dry-run checks

- [ ] **Step 3: Verify SQL dry-run with condition**

Run:

```bash
rtk proxy make -n phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
```

Expected command shape:

```text
npm run phase:sql -- --phase phase-06 --scenario review-aggregate --condition naive-index --action prepare
```

- [ ] **Step 4: Verify SQL dry-run without condition for data profile**

Run:

```bash
rtk proxy make -n phase-sql PHASE=phase-06 SCENARIO=data-profile CONDITION=ignored ACTION=profile
```

Expected command shape:

```text
npm run phase:sql -- --phase phase-06 --scenario data-profile --action profile
```

- [ ] **Step 5: Verify k6 dry-run with existing `PRESET`**

Run:

```bash
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index
```

Expected command shape:

```text
npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --mode prometheus --condition naive-index
```

- [ ] **Step 6: Verify k6 dry-run with `K6_PRESET` override**

Run:

```bash
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=ignored K6_PRESET=review-summary-baseline CONDITION=naive-index
```

Expected command shape:

```text
npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --mode prometheus --condition naive-index
```

- [ ] **Step 7: Verify Grafana dry-run with existing `PRESET`**

Run:

```bash
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index TABLE=review
```

Expected command shape:

```text
npm run grafana:capture -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --table review --no-align-phase-rows
```

- [ ] **Step 8: Verify Grafana dry-run with `GRAFANA_PRESET` override**

Run:

```bash
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=ignored GRAFANA_PRESET=baseline CONDITION=naive-index TABLE=review
```

Expected command shape:

```text
npm run grafana:capture -- --phase phase-06 --scenario review-summary --preset baseline --pool pool10 --table review --no-align-phase-rows
```

- [ ] **Step 9: Verify dashboard generation**

Run:

```bash
rtk proxy make grafana-generate
```

Expected:

- command exits 0
- `docker/grafana/dashboards/db-lab-overview.json` remains valid JSON

- [ ] **Step 10: Verify observability contract**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: command exits 0.

- [ ] **Step 11: Commit verification docs or fixes**

If this slice required file changes:

```bash
git add Makefile makefiles docs/guides/commands.md docs/guides/project-format-standard.md
git commit -m "test(make): verify command interface split"
```

If no files changed, do not commit.
