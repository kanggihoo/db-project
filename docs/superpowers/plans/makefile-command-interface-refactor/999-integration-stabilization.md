# 999 Integration Stabilization

## Goal

Makefile command interface 리팩토링 전체가 기존 공개 명령, 문서, 검증 계약을 유지하는지 최종 점검한다.

## Files

- Inspect: `Makefile`
- Inspect: `makefiles/*.mk`
- Inspect: `docs/guides/commands.md`
- Inspect: `docs/guides/project-format-standard.md`
- Inspect: `scripts/run-k6-evidence.mjs`
- Inspect: `scripts/capture-grafana-dashboard.mjs`
- Inspect: `scripts/run-phase-sql.mjs`
- Inspect: `docker/grafana/dashboards/db-lab-overview.json`

## Steps

- [ ] **Step 1: Review Makefile include graph**

Run:

```bash
rtk read Makefile
rtk proxy find makefiles -maxdepth 1 -type f -print | sort
```

Expected:

- root `Makefile` contains shell/default goal/include declarations only
- `makefiles/config.mk`, `help.mk`, `env.mk`, `db.mk`, `grafana.mk`, `k6.mk`, `evidence.mk`, `sql.mk` exist
- `makefiles/phase-compat.mk` does not exist in this refactor

- [ ] **Step 2: Run official help target**

Run:

```bash
rtk proxy make help
```

Expected: command exits 0.

- [ ] **Step 3: Run official dashboard generation target**

Run:

```bash
rtk proxy make grafana-generate
```

Expected:

- command exits 0
- dashboard JSON is regenerated

- [ ] **Step 4: Run observability verifier**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: command exits 0.

- [ ] **Step 5: Run full dry-run suite**

Run:

```bash
rtk proxy make -n phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
rtk proxy make -n phase-sql PHASE=phase-06 SCENARIO=data-profile CONDITION=ignored ACTION=profile
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=ignored K6_PRESET=review-summary-baseline CONDITION=naive-index
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index TABLE=review
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=ignored GRAFANA_PRESET=baseline CONDITION=naive-index TABLE=review
rtk proxy make -n evidence-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=query-shaped-index TABLE=review
```

Expected:

- all commands expand without Makefile errors
- SQL data profile command omits `--condition`
- k6 commands use the k6 preset value
- Grafana direct capture uses the Grafana preset value
- phase-06 capture includes `--no-align-phase-rows`

- [ ] **Step 6: Check docs and formatting**

Run:

```bash
rtk git diff --check
rtk grep "K6_PRESET\\|GRAFANA_PRESET\\|makefiles/" docs/guides Makefile makefiles
```

Expected:

- no whitespace errors
- docs mention the new preset variables and makefiles split

- [ ] **Step 7: Review diff for scope creep**

Run:

```bash
rtk git diff
```

Check for unintended changes:

- changed Node wrapper CLI contracts
- added Phase 3/4 compatibility targets
- changed k6 scenario files
- changed Grafana YAML source
- changed dashboard semantics unrelated to `make grafana-generate`
- changed phase evidence directory layout

- [ ] **Step 8: Commit final changes**

```bash
git add Makefile makefiles docs/guides/commands.md docs/guides/project-format-standard.md docs/superpowers/specs/2026-06-02-makefile-command-interface-refactor-design.md docs/superpowers/plans/makefile-command-interface-refactor
git commit -m "refactor(make): split command interface into includes"
```
