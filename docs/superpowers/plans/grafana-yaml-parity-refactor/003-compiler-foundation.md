# 003 Compiler Foundation

## Goal

YAML source를 Grafana dashboard JSON으로 변환하는 compiler 기반을 구현한다.

## Files

- Create: `scripts/grafana/lib/yaml-loader.mjs`
- Create: `scripts/grafana/lib/query-registry.mjs`
- Create: `scripts/grafana/lib/grafana-builder.mjs`
- Create: `scripts/grafana/lib/layout.mjs`
- Create: `scripts/grafana/lib/dashboard-compiler.mjs`
- Create: `scripts/grafana/lib/write-dashboard.mjs`

## Steps

- [ ] **Step 1: Implement YAML loader**

Create `scripts/grafana/lib/yaml-loader.mjs`.

Requirements:

- use the `yaml` package
- read UTF-8 files
- return parsed JavaScript objects
- include file path in parse/load error messages

- [ ] **Step 2: Implement query registry**

Create `scripts/grafana/lib/query-registry.mjs`.

Requirements:

- load all `scripts/grafana/queries/*.yml`
- build alias-to-query map
- fail on duplicate query alias
- fail when a row panel references a missing query alias

- [ ] **Step 3: Implement Grafana builder**

Create `scripts/grafana/lib/grafana-builder.mjs`.

Port existing builder responsibilities from `scripts/generate-db-lab-dashboard.mjs`:

- datasource object
- target creation
- sequential `refId`
- stat panel
- timeseries panel
- table panel
- row panel
- default field config and options
- no-data fallback helper

Keep datasource:

```javascript
{ type: 'prometheus', uid: 'prometheus' }
```

- [ ] **Step 4: Implement layout helper**

Create `scripts/grafana/lib/layout.mjs`.

Requirements:

- row header uses width 24 and height 1
- explicit panel layout overrides are honored
- `layout.yOffset` is relative to the row content start
- fallback layout exists for panels without explicit layout

Parity-first means explicit layout should be used for most current panels.

- [ ] **Step 5: Implement dashboard compiler**

Create `scripts/grafana/lib/dashboard-compiler.mjs`.

Requirements:

- load dashboard YAML
- load row YAML files listed by dashboard YAML
- resolve panel query aliases through query registry
- compile variables into Grafana templating list
- compile rows and panels into Grafana `panels`
- preserve existing common dashboard fields:
  - `schemaVersion`
  - `refresh`
  - `time`
  - `timezone`
  - `tags`
  - annotations

- [ ] **Step 6: Implement dashboard writer**

Create `scripts/grafana/lib/write-dashboard.mjs`.

Requirements:

- create output directory recursively
- write pretty JSON with trailing newline
- log generated output path

- [ ] **Step 7: Add narrow module-level smoke command if useful**

If useful, add a temporary or direct command to compile without replacing the old generator yet:

```bash
rtk proxy node scripts/grafana/generate.mjs
```

Do not change `scripts/generate-db-lab-dashboard.mjs` in this slice unless compiler entrypoint is already ready.

- [ ] **Step 8: Commit**

```bash
git add scripts/grafana/lib
git commit -m "chore(grafana): add yaml dashboard compiler"
```
