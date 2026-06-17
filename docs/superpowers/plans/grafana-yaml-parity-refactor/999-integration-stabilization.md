# 999 Integration Stabilization

## Goal

Grafana YAML parity refactor 전체가 기존 dashboard, provisioning, capture, verification 계약을 유지하는지 최종 점검한다.

## Files

- Inspect: `scripts/grafana/**/*.mjs`
- Inspect: `scripts/grafana/**/*.yml`
- Inspect: `scripts/generate-db-lab-dashboard.mjs`
- Inspect: `docker/grafana/dashboards/db-lab-overview.json`
- Inspect: `scripts/verify-observability.mjs`
- Inspect: `Makefile`
- Inspect: `package.json`
- Inspect: `package-lock.json`

## Steps

- [ ] **Step 1: Generate dashboard through official target**

Run:

```bash
rtk proxy make grafana-generate
```

Expected:

- command exits 0
- `docker/grafana/dashboards/db-lab-overview.json` is regenerated

- [ ] **Step 2: Run observability verification**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: command exits 0.

- [ ] **Step 3: Verify generated JSON is parseable**

Run:

```bash
rtk proxy node -e "const d=require('./docker/grafana/dashboards/db-lab-overview.json'); console.log(d.uid, d.title, d.panels.length)"
```

Expected:

- uid is `db-lab-overview`
- title is `DB Lab Overview`
- panel count is non-zero

- [ ] **Step 4: Verify YAML references**

Run:

```bash
rtk rg -n "query:|uid: db-lab-overview|title: DB Lab Overview|phase|scenario|preset|pool|uri|table" scripts/grafana
```

Expected:

- YAML source contains dashboard variables and panel query aliases.

- [ ] **Step 5: Review diff for unintended dashboard redesign**

Run:

```bash
rtk git diff
```

Check for unintended changes:

- changed dashboard uid/title
- removed variables
- removed required row titles
- changed PromQL meaning
- changed capture script CLI options
- changed provisioning path
- row/panel redesign beyond parity-first scope

- [ ] **Step 6: Optional Grafana provisioning smoke**

If Docker/Grafana is available:

```bash
rtk docker compose up -d grafana prometheus
rtk docker compose ps grafana
```

If Grafana is not available, record that provisioning smoke was not run.

- [ ] **Step 7: Commit final stabilization changes**

```bash
git add scripts/grafana scripts/generate-db-lab-dashboard.mjs docker/grafana/dashboards/db-lab-overview.json scripts/verify-observability.mjs package.json package-lock.json docs/superpowers/specs/2026-06-02-grafana-yaml-parity-refactor-design.md docs/superpowers/plans/grafana-yaml-parity-refactor
git commit -m "chore(grafana): stabilize yaml parity refactor"
```
