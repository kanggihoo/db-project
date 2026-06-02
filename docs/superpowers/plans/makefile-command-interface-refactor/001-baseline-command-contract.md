# 001 Baseline Command Contract

## Goal

현재 Makefile 공개 target과 명령 확장 결과를 기준선으로 확인한다.

## Files

- Inspect: `Makefile`
- Inspect: `package.json`
- Inspect: `scripts/run-k6-evidence.mjs`
- Inspect: `scripts/capture-grafana-dashboard.mjs`
- Inspect: `scripts/run-phase-sql.mjs`

## Steps

- [ ] **Step 1: Confirm current branch and uncommitted files**

Run:

```bash
rtk git status --short --branch
```

Expected:

- branch is `codex/refactor-tooling`
- unrelated root reference specs are not staged
- unrelated ADR files are not staged unless the user explicitly asks

- [ ] **Step 2: Capture current help output**

Run:

```bash
rtk proxy make help
```

Expected:

- command exits 0
- output includes `env-check`, `db-start`, `db-shell`, `grafana-generate`, `phase-sql`, `k6-evidence`, `grafana-capture`, `evidence-capture`, `phase-status`

- [ ] **Step 3: Capture current SQL dry-run**

Run:

```bash
rtk proxy make -n phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
```

Expected command shape:

```text
npm run phase:sql -- --phase phase-06 --scenario review-aggregate --condition naive-index --action prepare
```

- [ ] **Step 4: Capture current data-profile SQL dry-run**

Run:

```bash
rtk proxy make -n phase-sql PHASE=phase-06 SCENARIO=data-profile CONDITION=ignored ACTION=profile
```

Expected command shape:

```text
npm run phase:sql -- --phase phase-06 --scenario data-profile --action profile
```

The command must not include `--condition ignored`.

- [ ] **Step 5: Capture current k6 dry-run**

Run:

```bash
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index
```

Expected command shape:

```text
npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --mode prometheus --condition naive-index
```

- [ ] **Step 6: Capture current Grafana dry-run**

Run:

```bash
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index TABLE=review
```

Expected command shape:

```text
npm run grafana:capture -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --table review --no-align-phase-rows
```

- [ ] **Step 7: Capture current evidence dry-run**

Run:

```bash
rtk proxy make -n evidence-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=query-shaped-index TABLE=review
```

Expected command shape:

```text
npm run evidence:capture -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --mode prometheus --condition query-shaped-index --table review
```

- [ ] **Step 8: Record baseline result**

No source changes are expected in this slice. Do not commit unless a baseline note file is explicitly requested.
