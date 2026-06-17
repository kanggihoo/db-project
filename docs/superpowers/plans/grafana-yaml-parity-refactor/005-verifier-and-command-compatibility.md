# 005 Verifier And Command Compatibility

## Goal

YAML generator 전환 후 기존 명령과 verifier가 새 구조를 안정적으로 검증하게 한다.

## Files

- Modify if needed: `scripts/verify-observability.mjs`
- Modify if needed: `package.json`
- Inspect: `Makefile`
- Inspect: `scripts/capture-grafana-dashboard.mjs`
- Inspect: `scripts/grafana-capture-utils.mjs`

## Steps

- [ ] **Step 1: Verify Makefile target**

Run:

```bash
rtk proxy make grafana-generate
```

Expected:

- Makefile still calls `node scripts/generate-db-lab-dashboard.mjs`
- generated output path remains unchanged

- [ ] **Step 2: Verify observability checker**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

If verifier fails due to structural but not semantic JSON differences, update it narrowly.

Verifier should still check:

- dashboard uid
- dashboard title
- variables
- required rows
- important metric expressions
- no-data fallback
- provisioning files

- [ ] **Step 3: Decide whether npm script is needed**

Current project uses `make grafana-generate` and direct node command. If adding an npm script improves consistency, add:

```json
"grafana:generate": "node scripts/generate-db-lab-dashboard.mjs"
```

Do not add it if it creates churn without a current caller.

- [ ] **Step 4: Verify capture script contract is unchanged**

Inspect capture URL helper:

```bash
rtk rg -n "db-lab-overview|var-phase|var-scenario|var-preset|var-pool|var-uri|var-table" scripts/capture-grafana-dashboard.mjs scripts/grafana-capture-utils.mjs
```

Expected:

- capture still targets `db-lab-overview`
- variable names remain unchanged

- [ ] **Step 5: Optional live capture**

If Grafana is running and a live capture is acceptable:

```bash
rtk npm run grafana:capture -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --live
```

If Grafana is not running, skip and record it in the final summary. Do not fake a capture pass.

- [ ] **Step 6: Commit**

```bash
git add scripts/verify-observability.mjs package.json package-lock.json Makefile scripts
git commit -m "chore(grafana): verify yaml dashboard generation"
```
