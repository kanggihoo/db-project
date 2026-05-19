# 007 k6 And Grafana Strategy Observability

## Goal

Make strategy visible in k6 metrics and Grafana so Phase 3 evidence can compare Lazy, Fetch Join, BatchSize, and EntityGraph runs under the same phase/scenario/preset/pool conditions.

## Files

- Modify: `k6/orders-test.js`
- Modify: `k6/run.sh`
- Modify: `scripts/generate-db-lab-dashboard.mjs`
- Modify: `docs/guides/grafana-observability.md`
- Modify: `docs/guides/k6-load-testing.md`

## Steps

- [ ] **Step 1: Add strategy env propagation to k6 runner**

Modify `k6/run.sh`:

```bash
STRATEGY="${STRATEGY:-lazy}"
```

Add to `K6_ARGS`:

```bash
-e STRATEGY="$STRATEGY"
```

- [ ] **Step 2: Add strategy query parameter and label**

Modify `k6/orders-test.js`:

```js
const STRATEGY = __ENV.STRATEGY || 'lazy';
```

Add to `commonTags`:

```js
strategy: STRATEGY,
```

Modify request URL:

```js
const res = http.get(`${BASE_URL}/api/orders?userId=${userId}&strategy=${STRATEGY}`, {
    timeout: TIMEOUT,
    tags: requestTags,
});
```

- [ ] **Step 3: Add strategy dashboard variable**

Modify `scripts/generate-db-lab-dashboard.mjs`.

Change:

```js
const k6Filter = 'phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"';
```

To:

```js
const k6Filter = 'phase="$phase", scenario="$scenario", preset="$preset", pool="$pool", strategy=~"$strategy"';
```

Add a templating variable after `$pool`:

```js
{
  current: { selected: true, text: 'All', value: '$__all' },
  datasource,
  definition: 'label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"}, strategy)',
  includeAll: true,
  label: 'Strategy',
  multi: true,
  name: 'strategy',
  options: [],
  query: { query: 'label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"}, strategy)', refId: 'PrometheusVariableQueryEditor-VariableQuery' },
  refresh: 1,
  sort: 1,
  type: 'query',
}
```

- [ ] **Step 4: Change Phase 3 focus row panels**

In `scripts/generate-db-lab-dashboard.mjs`, replace the generic Phase 3 row builder behavior with Phase 3-specific panels:

```js
addRowWithPanels(panels, 'Phase 3 N+1 Focus', (rowY, targetPanels) => {
  targetPanels.push(
    timeSeries('Phase 3 p95 by Strategy', `histogram_quantile(0.95, sum by (le, strategy) (rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, '{{strategy}}', 0, rowY),
    timeSeries('Phase 3 Error Rate by Strategy', `avg by (strategy) (k6_http_req_failed_rate{${k6Filter}})`, '{{strategy}}', 12, rowY),
    timeSeries('Phase 3 Dropped Iterations by Strategy', zeroWhenNoData(`sum by (strategy) (rate(k6_dropped_iterations_total{${k6Filter}}[$__rate_interval]))`), '{{strategy}}', 0, rowY + 8),
    timeSeries('Phase 3 Hikari Pending', 'hikaricp_connections_pending', 'pending', 12, rowY + 8),
  );
  y = rowY + 16;
});
```

Keep Phase 1, Phase 2, and Phase 7 focus rows intact.

- [ ] **Step 5: Regenerate dashboard**

Run:

```bash
rtk npm run grafana:generate
```

Expected: `docker/grafana/dashboards/db-lab-overview.json` updates and contains `"name": "strategy"` and `"Phase 3 p95 by Strategy"`.

- [ ] **Step 6: Update docs**

In `docs/guides/grafana-observability.md`, add `strategy` to Measurement Conditions:

```markdown
| `strategy` | `lazy` | Low-cardinality Phase 3 loading strategy |
```

In `docs/guides/k6-load-testing.md`, add:

```markdown
Phase 3 orders runs can set `STRATEGY=lazy|fetch-join|batch-size|entity-graph`; the value is sent as a query parameter and a low-cardinality k6 label.
```

- [ ] **Step 7: Verify generated dashboard and scripts**

Run:

```bash
rtk grep "strategy" k6/run.sh k6/orders-test.js scripts/generate-db-lab-dashboard.mjs docker/grafana/dashboards/db-lab-overview.json
rtk grep "Phase 3 p95 by Strategy" docker/grafana/dashboards/db-lab-overview.json
```

Expected: both commands return matches.

- [ ] **Step 8: Commit**

```bash
git add k6/orders-test.js k6/run.sh scripts/generate-db-lab-dashboard.mjs docker/grafana/dashboards/db-lab-overview.json docs/guides/grafana-observability.md docs/guides/k6-load-testing.md
git commit -m "feat: add phase 3 strategy observability"
```
