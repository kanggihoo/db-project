# 001 First Vertical Slice: k6 Measurement Labels

## Goal

Add a repository verification script, then label k6 metrics with the Measurement Condition values needed by `DB Lab Overview`.

## Files

- Create: `scripts/verify-observability.mjs`
- Modify: `k6/run.sh`
- Modify: `k6/orders-test.js`
- Modify: `k6/products-test.js`
- Modify: `k6/points-test.js`

## Steps

- [ ] **Step 1: Create the failing verification script**

Create `scripts/verify-observability.mjs` with checks for:

```js
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));

function pathOf(relativePath) {
  return join(root, relativePath);
}

function read(relativePath) {
  return readFileSync(pathOf(relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function assertIncludes(file, expected) {
  const content = read(file);
  assert(content.includes(expected), `${file} must include ${expected}`);
}

function verifyRunScript() {
  const content = read('k6/run.sh');
  for (const expected of [
    'PHASE="${PHASE:-phase-01}"',
    'POOL="${POOL:-pool10}"',
    '-e PHASE="$PHASE"',
    '-e SCENARIO="$SCENARIO"',
    '-e PRESET_NAME="$PRESET"',
    '-e POOL="$POOL"',
  ]) {
    assert(content.includes(expected), `k6/run.sh must include ${expected}`);
  }
}

function verifyScenario(file, scenario, requestName) {
  const content = read(file);
  for (const expected of [
    `scenario: __ENV.SCENARIO || '${scenario}'`,
    "phase: __ENV.PHASE || 'phase-01'",
    "preset: __ENV.PRESET_NAME || 'baseline'",
    "pool: __ENV.POOL || 'pool10'",
    "systemTags: ['status', 'method', 'name', 'expected_response']",
    `name: '${requestName}'`,
    'tags: requestTags',
  ]) {
    assert(content.includes(expected), `${file} must include ${expected}`);
  }
}

function verifyGrafanaProvisioning() {
  assertIncludes('docker/grafana/provisioning/datasources/prometheus.yml', 'uid: prometheus');
  assertIncludes('docker/grafana/provisioning/dashboards/dashboards.yml', 'path: /var/lib/grafana/dashboards');
}

function verifyDashboard() {
  const dashboardPath = 'docker/grafana/dashboards/db-lab-overview.json';
  assert(existsSync(pathOf(dashboardPath)), `${dashboardPath} must exist`);
  const dashboard = JSON.parse(read(dashboardPath));
  assert(dashboard.uid === 'db-lab-overview', 'dashboard uid must be db-lab-overview');
  assert(dashboard.title === 'DB Lab Overview', 'dashboard title must be DB Lab Overview');
  const rows = dashboard.panels.filter((panel) => panel.type === 'row').map((panel) => panel.title);
  for (const title of ['Run Summary', 'k6 Load', 'Spring API', 'Hikari Pool', 'PostgreSQL Activity', 'Table Access', 'Phase 1 Baseline Focus']) {
    assert(rows.includes(title), `dashboard must include row ${title}`);
  }
  const dashboardText = JSON.stringify(dashboard);
  for (const expected of ['$phase', '$scenario', '$preset', '$pool', 'k6_http_reqs_total', 'k6_http_req_duration_seconds', 'hikaricp_connections_pending', 'pg_stat_user_tables_seq_scan']) {
    assert(dashboardText.includes(expected), `dashboard must include ${expected}`);
  }
}

function verifySpringHistograms() {
  const content = read('ecommerce/src/main/resources/application.yaml');
  assert(content.includes('percentiles-histogram:'), 'application.yaml must enable percentiles-histogram');
  assert(content.includes('http.server.requests: true'), 'application.yaml must enable HTTP request histograms');
}

function verifyDocs() {
  assertIncludes('docs/guides/grafana-observability.md', 'DB Lab Overview');
  assertIncludes('docs/guides/grafana-observability.md', 'db-lab-grafana-dashboard-spec.md');
  assertIncludes('docs/guides/k6-load-testing.md', 'PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus');
  assertIncludes('docs/guides/environment.md', 'DB Lab Overview');
}

verifyRunScript();
verifyScenario('k6/orders-test.js', 'orders', 'GET /api/orders');
verifyScenario('k6/products-test.js', 'products', 'GET /api/products');
verifyScenario('k6/points-test.js', 'points', 'GET /api/points');
verifyGrafanaProvisioning();
verifyDashboard();
verifySpringHistograms();
verifyDocs();

console.log('Observability configuration verified.');
```

- [ ] **Step 2: Verify RED**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: FAIL with `k6/run.sh must include PHASE="${PHASE:-phase-01}"`.

- [ ] **Step 3: Update `k6/run.sh`**

Add `PHASE="${PHASE:-phase-01}"`, `POOL="${POOL:-pool10}"`, and pass these k6 arguments in local, Docker, and Prometheus modes:

```bash
K6_ARGS=(
  -e "PRESET=$PRESET_FILE"
  -e "PHASE=$PHASE"
  -e "SCENARIO=$SCENARIO"
  -e "PRESET_NAME=$PRESET"
  -e "POOL=$POOL"
)
```

Every `k6 run` invocation must include `"${K6_ARGS[@]}"`.

- [ ] **Step 4: Update each k6 scenario**

Add this tag structure to each `k6/*-test.js`, changing only the fallback scenario and request name:

```js
const commonTags = {
    phase: __ENV.PHASE || 'phase-01',
    scenario: __ENV.SCENARIO || 'orders',
    preset: __ENV.PRESET_NAME || 'baseline',
    pool: __ENV.POOL || 'pool10',
};

const requestTags = {
    ...commonTags,
    name: 'GET /api/orders',
};

export const options = {
    tags: commonTags,
    systemTags: ['status', 'method', 'name', 'expected_response'],
    scenarios: {
        steady: {
            executor: 'constant-arrival-rate',
            rate: Number(preset.rate || 50),
            timeUnit: '1s',
            duration: preset.duration || '5m',
            preAllocatedVUs: Number(preset.preAllocatedVUs || 100),
            maxVUs: Number(preset.maxVUs || 300),
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],
    },
};
```

Request calls must include:

```js
{
    timeout: TIMEOUT,
    tags: requestTags,
}
```

Use these scenario-specific values:

| File | Scenario fallback | Request name |
|---|---|---|
| `k6/orders-test.js` | `orders` | `GET /api/orders` |
| `k6/products-test.js` | `products` | `GET /api/products` |
| `k6/points-test.js` | `points` | `GET /api/points` |

- [ ] **Step 5: Verify slice**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: FAIL for the next missing layer, such as datasource UID, dashboard JSON, Spring histograms, or docs. It must not fail for `k6/run.sh` or `k6/*.js`.

- [ ] **Step 6: Commit**

```bash
git add scripts/verify-observability.mjs k6/run.sh k6/orders-test.js k6/products-test.js k6/points-test.js
git commit -m "feat: label k6 observability metrics"
```
