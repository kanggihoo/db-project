# 007 k6 Strategy Evidence Naming

## Goal

Run Phase 3 order load tests with different loading strategies by passing `STRATEGY` to k6, then keep each strategy's Phase Evidence separate through the evidence condition directory and Grafana screenshot filename.

## Decision

Do not add a Grafana `strategy` variable or strategy-by-strategy dashboard panels in this slice. The shared `DB Lab Overview` dashboard already has common k6, Spring API, Hikari, and PostgreSQL rows. Phase 3 strategy comparison will use separate fixed run windows, condition names, screenshots, k6 summaries, and `pg_stat_statements` snapshots.

## Prerequisite

Execute this plan after the Phase 2 shared observability tooling has been brought into the Phase 3 branch. Required shared files include `k6/run.sh`, `package.json`, `scripts/run-k6-evidence.mjs`, `scripts/capture-grafana-dashboard.mjs`, `scripts/grafana-capture-utils.mjs`, and `docs/guides/scripts.md`.

## Files

- Modify: `k6/run.sh`
- Modify: `k6/orders-test.js`
- Modify: `docs/guides/scripts.md`
- Modify: `docs/guides/k6-load-testing.md`

## Steps

- [ ] **Step 1: Add strategy env propagation to k6 runner**

Modify `k6/run.sh` near the existing `PHASE` and `POOL` defaults:

```bash
STRATEGY="${STRATEGY:-lazy}"
```

Add `STRATEGY` to `K6_ARGS`:

```bash
K6_ARGS=(
  -e PRESET="$PRESET_FILE"
  -e PHASE="$PHASE"
  -e SCENARIO="$SCENARIO"
  -e PRESET_NAME="$PRESET"
  -e POOL="$POOL"
  -e STRATEGY="$STRATEGY"
)
```

- [ ] **Step 2: Add strategy query parameter to the orders scenario**

Modify `k6/orders-test.js`:

```js
const STRATEGY = __ENV.STRATEGY || 'lazy';
```

Modify the request URL:

```js
const res = http.get(`${BASE_URL}/api/orders?userId=${userId}&strategy=${STRATEGY}`, {
    timeout: TIMEOUT,
    tags: requestTags,
});
```

Do not add `strategy` to `commonTags` in this plan. Strategy separation is handled by evidence condition names and fixed `run-window.json` capture windows.

- [ ] **Step 3: Document strategy-specific evidence capture**

Update `docs/guides/scripts.md` in the `k6/run.sh` or evidence wrapper section with Phase 3 examples:

```bash
STRATEGY=lazy rtk npm run evidence:capture -- \
  --phase phase-03 \
  --scenario orders \
  --condition pool10-lazy \
  --table orders \
  --output docs/evidence/phase-03/grafana-screenshots/orders-pool10-lazy.png

STRATEGY=fetch-join rtk npm run evidence:capture -- \
  --phase phase-03 \
  --scenario orders \
  --condition pool10-fetch-join \
  --table orders \
  --output docs/evidence/phase-03/grafana-screenshots/orders-pool10-fetch-join.png

STRATEGY=batch-size rtk npm run evidence:capture -- \
  --phase phase-03 \
  --scenario orders \
  --condition pool10-batch-size \
  --table orders \
  --output docs/evidence/phase-03/grafana-screenshots/orders-pool10-batch-size.png

STRATEGY=entity-graph rtk npm run evidence:capture -- \
  --phase phase-03 \
  --scenario orders \
  --condition pool10-entity-graph \
  --table orders \
  --output docs/evidence/phase-03/grafana-screenshots/orders-pool10-entity-graph.png
```

State that `evidence:capture` passes the current environment through to `k6/run.sh`, so `STRATEGY=...` controls the API strategy while `--condition` and `--output` control evidence naming.

- [ ] **Step 4: Document Phase 3 strategy usage in the k6 guide**

Update `docs/guides/k6-load-testing.md`:

```markdown
Phase 3 orders runs can set `STRATEGY=lazy|fetch-join|batch-size|entity-graph`.
The value is sent to `GET /api/orders` as the `strategy` query parameter.
Keep strategy evidence separate by using matching `--condition` and `--output` names, for example `pool10-lazy` and `orders-pool10-lazy.png`.
```

- [ ] **Step 5: Verify runner and scenario changes**

Run:

```bash
rtk grep "STRATEGY" k6/run.sh k6/orders-test.js
```

Expected: matches show `STRATEGY` defaulting in `k6/run.sh`, being passed in `K6_ARGS`, and being read by `k6/orders-test.js`.

- [ ] **Step 6: Verify docs**

Run:

```bash
rtk grep "pool10-lazy" docs/guides/scripts.md docs/guides/k6-load-testing.md
rtk grep "entity-graph" docs/guides/scripts.md docs/guides/k6-load-testing.md
```

Expected: both commands return matches.

- [ ] **Step 7: Commit**

```bash
git add k6/run.sh k6/orders-test.js docs/guides/scripts.md docs/guides/k6-load-testing.md docs/superpowers/plans/phase-03-n-plus-one/007-k6-and-grafana-strategy-observability.md
git commit -m "docs: simplify phase 3 strategy evidence plan"
```
