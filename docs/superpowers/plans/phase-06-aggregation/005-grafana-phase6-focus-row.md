# 005 Grafana Phase 6 Focus Row

## Goal

Keep one shared Grafana dashboard while making Phase 6 easy to capture and review through a dedicated `Phase 6 Aggregation Focus` row.

## Files

- Modify: `scripts/generate-db-lab-dashboard.mjs`
- Modify: `scripts/grafana-capture-utils.mjs`
- Modify: `scripts/grafana-capture-utils.test.mjs`
- Modify generated: `docker/grafana/dashboards/db-lab-overview.json`
- Modify: `docs/guides/grafana-observability.md`
- Modify: `docs/guides/scripts.md`

## Steps

- [ ] **Step 1: Add the Phase 6 focus row to the dashboard generator**

Modify the focus row list in `scripts/generate-db-lab-dashboard.mjs` so it includes Phase 6 between Phase 4 and Phase 7:

```js
for (const title of [
  'Phase 1 Baseline Focus',
  'Phase 2 Index Focus',
  'Phase 3 N+1 Focus',
  'Phase 4 Transaction Focus',
  'Phase 6 Aggregation Focus',
  'Phase 7 Pagination Focus',
]) {
  dashboard.panels.push(row(title));
  dashboard.panels.push(...createFocusPanels(title));
}
```

Expected: the generator still creates one `DB Lab Overview` dashboard.

- [ ] **Step 2: Add `phase-06` capture mapping**

Modify `scripts/grafana-capture-utils.mjs`:

```js
const PHASE_FOCUS_ROWS = {
  'phase-01': 'Phase 1 Baseline Focus',
  'phase-02': 'Phase 2 Index Focus',
  'phase-03': 'Phase 3 N+1 Focus',
  'phase-04': 'Phase 4 Transaction Focus',
  'phase-06': 'Phase 6 Aggregation Focus',
  'phase-07': 'Phase 7 Pagination Focus',
};
```

Expected: `capture-grafana-dashboard.mjs --phase phase-06` expands the Phase 6 focus row instead of falling back to a generic view.

- [ ] **Step 3: Add the regression test**

Modify `scripts/grafana-capture-utils.test.mjs` in the existing phase mapping test:

```js
assert.equal(getFocusRowTitleForPhase('phase-06'), 'Phase 6 Aggregation Focus');
```

Expected: the test protects the Phase 6 mapping from being removed.

- [ ] **Step 4: Regenerate the shared dashboard JSON**

Run:

```bash
rtk node scripts/generate-db-lab-dashboard.mjs
```

Expected: `docker/grafana/dashboards/db-lab-overview.json` contains `Phase 6 Aggregation Focus`.

- [ ] **Step 5: Update Grafana guide**

Modify `docs/guides/grafana-observability.md` so the shared-dashboard section says Phase 6 uses the common `DB Lab Overview` dashboard with a `Phase 6 Aggregation Focus` row. Make clear that query-level evidence remains in `EXPLAIN ANALYZE` and `pg_stat_statements` files, not new Prometheus labels.

- [ ] **Step 6: Update script guide supported phases**

Modify `docs/guides/scripts.md` so the `--phase <phase-id>` row includes `phase-06`:

```markdown
| `--phase <phase-id>` | Set `var-phase` and the phase focus row to expand. Supported: `phase-01`, `phase-02`, `phase-03`, `phase-04`, `phase-06`, `phase-07`. |
```

- [ ] **Step 7: Verify Grafana mapping and dashboard**

Run:

```bash
rtk node --test scripts/grafana-capture-utils.test.mjs
rtk node scripts/verify-observability.mjs
rtk rg -n "Phase 6 Aggregation Focus|phase-06" scripts docker/grafana/dashboards docs/guides
```

Expected: tests pass and output includes generator, capture mapping, generated dashboard, and guide references.

- [ ] **Step 8: Commit**

```bash
git add scripts/generate-db-lab-dashboard.mjs scripts/grafana-capture-utils.mjs scripts/grafana-capture-utils.test.mjs docker/grafana/dashboards/db-lab-overview.json docs/guides/grafana-observability.md docs/guides/scripts.md
git commit -m "chore(observability): add phase 6 grafana focus row"
```

