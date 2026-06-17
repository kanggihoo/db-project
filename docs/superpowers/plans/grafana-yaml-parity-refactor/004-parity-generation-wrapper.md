# 004 Parity Generation Wrapper

## Goal

새 YAML compiler로 `docker/grafana/dashboards/db-lab-overview.json`을 생성하고, 기존 `scripts/generate-db-lab-dashboard.mjs`를 compatibility wrapper로 바꾼다.

## Files

- Create: `scripts/grafana/generate.mjs`
- Modify: `scripts/generate-db-lab-dashboard.mjs`
- Modify generated: `docker/grafana/dashboards/db-lab-overview.json`

## Steps

- [ ] **Step 1: Create `scripts/grafana/generate.mjs`**

Implement entrypoint:

- load `scripts/grafana/dashboards/db-lab-overview.yml`
- compile dashboard JSON
- write to `docker/grafana/dashboards/db-lab-overview.json`

- [ ] **Step 2: Replace old generator with wrapper**

Modify `scripts/generate-db-lab-dashboard.mjs` so it only delegates to the new generator.

Acceptable shape:

```javascript
import './grafana/generate.mjs';
```

If `generate.mjs` exports a function instead, call that function explicitly.

- [ ] **Step 3: Generate dashboard**

Run:

```bash
rtk proxy node scripts/generate-db-lab-dashboard.mjs
```

Expected:

- output path remains `docker/grafana/dashboards/db-lab-overview.json`
- command exits 0

- [ ] **Step 4: Compare semantic contract**

Run:

```bash
rtk proxy node -e "const d=require('./docker/grafana/dashboards/db-lab-overview.json'); console.log(JSON.stringify({uid:d.uid,title:d.title,rows:d.panels.filter(p=>p.type==='row').map(p=>p.title),variables:d.templating.list.map(v=>v.name)}, null, 2))"
```

Expected:

- uid remains `db-lab-overview`
- title remains `DB Lab Overview`
- variables include `phase`, `scenario`, `preset`, `pool`, `uri`, `table`
- current row titles remain present

- [ ] **Step 5: Run verifier**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: command exits 0.

- [ ] **Step 6: Commit**

```bash
git add scripts/grafana/generate.mjs scripts/generate-db-lab-dashboard.mjs docker/grafana/dashboards/db-lab-overview.json
git commit -m "chore(grafana): generate db lab dashboard from yaml"
```
