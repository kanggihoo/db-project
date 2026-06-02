# 001 Baseline Contract And Dependency

## Goal

YAML 전환 전에 현재 dashboard 생성 계약을 기준선으로 고정하고 YAML parser dependency를 추가한다.

## Files

- Modify: `package.json`
- Modify: `package-lock.json`
- Inspect: `scripts/generate-db-lab-dashboard.mjs`
- Inspect: `docker/grafana/dashboards/db-lab-overview.json`
- Inspect: `scripts/verify-observability.mjs`

## Steps

- [ ] **Step 1: Capture current generator output contract**

Run:

```bash
rtk proxy node scripts/generate-db-lab-dashboard.mjs
rtk proxy node scripts/verify-observability.mjs
```

Expected:

- `docker/grafana/dashboards/db-lab-overview.json` is generated.
- `scripts/verify-observability.mjs` exits 0.

- [ ] **Step 2: Record semantic contract**

Inspect generated JSON and confirm:

```bash
rtk proxy node -e "const d=require('./docker/grafana/dashboards/db-lab-overview.json'); console.log(JSON.stringify({uid:d.uid,title:d.title,rows:d.panels.filter(p=>p.type==='row').map(p=>p.title),variables:d.templating.list.map(v=>v.name)}, null, 2))"
```

Expected:

- uid is `db-lab-overview`.
- title is `DB Lab Overview`.
- variables include `phase`, `scenario`, `preset`, `pool`, `uri`, `table`.
- existing row titles are visible.

- [ ] **Step 3: Add YAML dependency**

Run:

```bash
rtk npm install --save-dev yaml
```

Expected:

- `package.json` includes `yaml` in `devDependencies`.
- `package-lock.json` is updated.

- [ ] **Step 4: Avoid changing runtime scripts yet**

Do not modify `scripts/generate-db-lab-dashboard.mjs` in this slice. This slice only prepares dependency and baseline.

- [ ] **Step 5: Verify dependency install did not break existing checks**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: command exits 0.

- [ ] **Step 6: Commit**

```bash
git add package.json package-lock.json
git commit -m "chore(grafana): add yaml parser dependency"
```
