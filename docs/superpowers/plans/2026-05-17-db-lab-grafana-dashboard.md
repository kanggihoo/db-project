# DB Lab Grafana Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provision a reusable `DB Lab Overview` Grafana dashboard and k6 label policy so each Learning Phase can capture comparable evidence.

**Architecture:** Keep one shared Grafana dashboard with common rows and Phase focus rows. Add low-cardinality k6 labels through environment variables, use Prometheus as the only Grafana datasource, and keep query-level SQL evidence in `pg_stat_statements` snapshots instead of Prometheus labels.

**Tech Stack:** Bash, k6 JavaScript, Spring Boot Actuator/Micrometer, Prometheus, Grafana provisioning JSON/YAML, Node.js verification scripts.

---

## Spec

Implement [DB Lab Grafana Dashboard Spec](../../guides/db-lab-grafana-dashboard-spec.md).

## File Structure

| Path | Responsibility |
|---|---|
| `scripts/verify-observability.mjs` | Fast repository-level verification for k6 labels, dashboard provisioning, and docs. |
| `scripts/generate-db-lab-dashboard.mjs` | Generates deterministic Grafana dashboard JSON. |
| `k6/run.sh` | Passes `PHASE`, `SCENARIO`, `PRESET_NAME`, and `POOL` to k6. |
| `k6/orders-test.js` | Adds stable low-cardinality tags for the orders scenario. |
| `k6/products-test.js` | Adds stable low-cardinality tags for the products scenario. |
| `k6/points-test.js` | Adds stable low-cardinality tags for the points scenario. |
| `docker/grafana/provisioning/datasources/prometheus.yml` | Gives the Prometheus datasource a stable UID. |
| `docker/grafana/dashboards/db-lab-overview.json` | Provisioned dashboard generated from the script. |
| `ecommerce/src/main/resources/application.yaml` | Enables histogram buckets needed for Spring p95 panels. |
| `docs/guides/grafana-observability.md` | Links the dashboard spec and usage model. |
| `docs/guides/k6-load-testing.md` | Documents labeled k6 execution. |
| `docs/guides/environment.md` | Documents dashboard provisioning and first verification. |

---

### Task 1: Add Observability Verification Script

**Files:**
- Create: `scripts/verify-observability.mjs`

- [ ] **Step 1: Write the failing verification script**

Create `scripts/verify-observability.mjs`:

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
  for (const title of [
    'Run Summary',
    'k6 Load',
    'Spring API',
    'Hikari Pool',
    'PostgreSQL Activity',
    'Table Access',
    'Phase 1 Baseline Focus',
  ]) {
    assert(rows.includes(title), `dashboard must include row ${title}`);
  }

  const dashboardText = JSON.stringify(dashboard);
  for (const expected of [
    '$phase',
    '$scenario',
    '$preset',
    '$pool',
    'k6_http_reqs_total',
    'k6_http_req_duration_seconds',
    'hikaricp_connections_pending',
    'pg_stat_user_tables_seq_scan',
  ]) {
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

- [ ] **Step 2: Run verification to confirm RED**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: FAIL with `k6/run.sh must include PHASE="${PHASE:-phase-01}"`.

- [ ] **Step 3: Commit**

```bash
git add scripts/verify-observability.mjs
git commit -m "test: add observability config verification"
```

---

### Task 2: Add Low-Cardinality k6 Labels

**Files:**
- Modify: `k6/run.sh`
- Modify: `k6/orders-test.js`
- Modify: `k6/products-test.js`
- Modify: `k6/points-test.js`
- Test: `scripts/verify-observability.mjs`

- [ ] **Step 1: Replace `k6/run.sh`**

Use this complete file:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SCENARIO="${1:-}"
PRESET="${2:-baseline}"
MODE="${3:-local}"
PHASE="${PHASE:-phase-01}"
POOL="${POOL:-pool10}"

if [[ -z "$SCENARIO" ]]; then
  echo "Usage: $0 <orders|products|points> [preset] [local|prometheus]" >&2
  exit 1
fi

SCRIPT="${SCENARIO}-test.js"
PRESET_FILE="presets/${PRESET}.json"

if [[ ! -f "$SCRIPT_DIR/$SCRIPT" ]]; then
  echo "Unknown k6 scenario: $SCENARIO" >&2
  exit 1
fi

if [[ ! -f "$SCRIPT_DIR/$PRESET_FILE" ]]; then
  echo "Unknown k6 preset: $PRESET" >&2
  exit 1
fi

K6_ARGS=(
  -e "PRESET=$PRESET_FILE"
  -e "PHASE=$PHASE"
  -e "SCENARIO=$SCENARIO"
  -e "PRESET_NAME=$PRESET"
  -e "POOL=$POOL"
)

case "$MODE" in
  local) ;;
  prometheus)
    cd "$ROOT_DIR"
    exec docker compose --profile test run --rm k6 \
      run \
      --out experimental-prometheus-rw \
      "${K6_ARGS[@]}" \
      "/scripts/$SCRIPT"
    ;;
  *)
    echo "Unknown k6 mode: $MODE" >&2
    echo "Usage: $0 <orders|products|points> [preset] [local|prometheus]" >&2
    exit 1
    ;;
esac

if command -v k6 >/dev/null 2>&1; then
  cd "$SCRIPT_DIR"
  exec k6 run "${K6_ARGS[@]}" "$SCRIPT"
fi

exec docker run --rm -i \
  --network host \
  -v "$SCRIPT_DIR:/scripts" \
  -w /scripts \
  grafana/k6 run "${K6_ARGS[@]}" "$SCRIPT"
```

- [ ] **Step 2: Replace `k6/orders-test.js`**

Use this complete file:

```js
import http from 'k6/http';
import { check } from 'k6';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/baseline.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const USER_START = Number(preset.userStart || 1);
const USER_END = Number(preset.userEnd || USER_START);
const TIMEOUT = preset.timeout || '5s';

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

function randomBetween(start, end) {
    return Math.floor(Math.random() * (end - start + 1)) + start;
}

export default function () {
    const userId = randomBetween(USER_START, USER_END);
    const res = http.get(`${BASE_URL}/api/orders?userId=${userId}`, {
        timeout: TIMEOUT,
        tags: requestTags,
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });
}
```

- [ ] **Step 3: Replace `k6/products-test.js`**

Use this complete file:

```js
import http from 'k6/http';
import { check } from 'k6';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/baseline.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const CATEGORY_START = Number(preset.categoryStart || 1);
const CATEGORY_END = Number(preset.categoryEnd || CATEGORY_START);
const STATUSES = preset.statuses || ['ON_SALE', 'SOLD_OUT', 'DISCONTINUED'];
const TIMEOUT = preset.timeout || '5s';

const commonTags = {
    phase: __ENV.PHASE || 'phase-01',
    scenario: __ENV.SCENARIO || 'products',
    preset: __ENV.PRESET_NAME || 'baseline',
    pool: __ENV.POOL || 'pool10',
};

const requestTags = {
    ...commonTags,
    name: 'GET /api/products',
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

function randomBetween(start, end) {
    return Math.floor(Math.random() * (end - start + 1)) + start;
}

export default function () {
    const categoryId = randomBetween(CATEGORY_START, CATEGORY_END);
    const status = STATUSES[Math.floor(Math.random() * STATUSES.length)];
    const res = http.get(`${BASE_URL}/api/products?categoryId=${categoryId}&status=${status}`, {
        timeout: TIMEOUT,
        tags: requestTags,
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });
}
```

- [ ] **Step 4: Replace `k6/points-test.js`**

Use this complete file:

```js
import http from 'k6/http';
import { check } from 'k6';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/baseline.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const USER_START = Number(preset.userStart || 1);
const USER_END = Number(preset.userEnd || USER_START);
const SIZE = Number(preset.size || 20);
const TIMEOUT = preset.timeout || '5s';

const commonTags = {
    phase: __ENV.PHASE || 'phase-01',
    scenario: __ENV.SCENARIO || 'points',
    preset: __ENV.PRESET_NAME || 'baseline',
    pool: __ENV.POOL || 'pool10',
};

const requestTags = {
    ...commonTags,
    name: 'GET /api/points',
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

function randomBetween(start, end) {
    return Math.floor(Math.random() * (end - start + 1)) + start;
}

function randomWeightedPage() {
    const rand = Math.random();
    if (rand < 0.5) {
        return randomBetween(1, 10);
    }
    if (rand < 0.8) {
        return randomBetween(50, 100);
    }
    return randomBetween(500, 1000);
}

export default function () {
    const userId = randomBetween(USER_START, USER_END);
    const page = preset.page === undefined ? randomWeightedPage() : Number(preset.page);
    const res = http.get(`${BASE_URL}/api/points?userId=${userId}&page=${page}&size=${SIZE}`, {
        timeout: TIMEOUT,
        tags: requestTags,
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });
}
```

- [ ] **Step 5: Run verification**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: still FAIL because Grafana datasource UID, dashboard JSON, Spring histograms, and docs are not complete yet. The failure must no longer mention `k6/run.sh` or `k6/*.js`.

- [ ] **Step 6: Commit**

```bash
git add k6/run.sh k6/orders-test.js k6/products-test.js k6/points-test.js
git commit -m "feat: label k6 observability metrics"
```

---

### Task 3: Stabilize Prometheus Datasource And Spring Histograms

**Files:**
- Modify: `docker/grafana/provisioning/datasources/prometheus.yml`
- Modify: `ecommerce/src/main/resources/application.yaml`
- Test: `scripts/verify-observability.mjs`

- [ ] **Step 1: Update Grafana datasource provisioning**

Replace `docker/grafana/provisioning/datasources/prometheus.yml` with:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

- [ ] **Step 2: Update Spring metrics config**

In `ecommerce/src/main/resources/application.yaml`, replace the `management:` block with:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
        hikaricp.connections.acquire: true
```

- [ ] **Step 3: Run verification**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: still FAIL because `docker/grafana/dashboards/db-lab-overview.json` and docs are not complete yet.

- [ ] **Step 4: Commit**

```bash
git add docker/grafana/provisioning/datasources/prometheus.yml ecommerce/src/main/resources/application.yaml
git commit -m "chore: stabilize observability metric inputs"
```

---

### Task 4: Generate DB Lab Overview Dashboard

**Files:**
- Create: `scripts/generate-db-lab-dashboard.mjs`
- Create: `docker/grafana/dashboards/db-lab-overview.json`
- Test: `scripts/verify-observability.mjs`

- [ ] **Step 1: Add the dashboard generator**

Create `scripts/generate-db-lab-dashboard.mjs`:

```js
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const outPath = join(root, 'docker/grafana/dashboards/db-lab-overview.json');

let panelId = 1;
let targetId = 1;

const datasource = {
  type: 'prometheus',
  uid: 'prometheus',
};

function target(expr, legendFormat = '') {
  return {
    datasource,
    editorMode: 'code',
    expr,
    legendFormat,
    range: true,
    refId: `A${targetId++}`,
  };
}

function grid(x, y, w, h) {
  return { x, y, w, h };
}

function row(title, y, collapsed = false) {
  return {
    id: panelId++,
    title,
    type: 'row',
    collapsed,
    datasource,
    gridPos: grid(0, y, 24, 1),
    panels: [],
  };
}

function stat(title, expr, x, y, w, h, unit = 'short') {
  return {
    id: panelId++,
    title,
    type: 'stat',
    datasource,
    gridPos: grid(x, y, w, h),
    targets: [target(expr)],
    options: {
      colorMode: 'value',
      graphMode: 'area',
      justifyMode: 'auto',
      orientation: 'auto',
      reduceOptions: {
        calcs: ['lastNotNull'],
        fields: '',
        values: false,
      },
      textMode: 'auto',
      wideLayout: true,
    },
    fieldConfig: {
      defaults: { unit },
      overrides: [],
    },
  };
}

function timeseries(title, targets, x, y, w, h, unit = 'short') {
  return {
    id: panelId++,
    title,
    type: 'timeseries',
    datasource,
    gridPos: grid(x, y, w, h),
    targets,
    fieldConfig: {
      defaults: {
        unit,
        custom: {
          drawStyle: 'line',
          lineInterpolation: 'linear',
          lineWidth: 1,
          fillOpacity: 8,
          showPoints: 'never',
          spanNulls: true,
        },
      },
      overrides: [],
    },
    options: {
      legend: {
        calcs: ['lastNotNull', 'max'],
        displayMode: 'table',
        placement: 'bottom',
        showLegend: true,
      },
      tooltip: {
        mode: 'multi',
        sort: 'none',
      },
    },
  };
}

function table(title, targets, x, y, w, h) {
  return {
    id: panelId++,
    title,
    type: 'table',
    datasource,
    gridPos: grid(x, y, w, h),
    targets,
    options: {
      showHeader: true,
      cellHeight: 'sm',
    },
    fieldConfig: {
      defaults: {},
      overrides: [],
    },
  };
}

const k6Filter = 'phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"';
const panels = [
  row('Run Summary', 0),
  stat('k6 p95', `histogram_quantile(0.95, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 0, 1, 4, 4, 's'),
  stat('k6 p99', `histogram_quantile(0.99, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 4, 1, 4, 4, 's'),
  stat('Error Rate', `avg(k6_http_req_failed_rate{${k6Filter}})`, 8, 1, 4, 4, 'percentunit'),
  stat('Actual RPS', `sum(rate(k6_http_reqs_total{${k6Filter}}[$__rate_interval]))`, 12, 1, 4, 4, 'reqps'),
  stat('Dropped Iterations', `sum(increase(k6_dropped_iterations_total{${k6Filter}}[$__range]))`, 16, 1, 4, 4, 'short'),
  stat('Hikari Pending Max', 'max(max_over_time(hikaricp_connections_pending[$__range]))', 20, 1, 4, 4, 'short'),

  row('k6 Load', 5),
  timeseries('Actual RPS', [target(`sum(rate(k6_http_reqs_total{${k6Filter}}[$__rate_interval]))`, 'rps')], 0, 6, 8, 7, 'reqps'),
  timeseries('k6 Latency', [
    target(`histogram_quantile(0.50, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 'p50'),
    target(`histogram_quantile(0.95, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 'p95'),
    target(`histogram_quantile(0.99, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 'p99'),
  ], 8, 6, 8, 7, 's'),
  timeseries('Error Rate And Dropped Iterations', [
    target(`avg(k6_http_req_failed_rate{${k6Filter}})`, 'error rate'),
    target(`sum(rate(k6_dropped_iterations_total{${k6Filter}}[$__rate_interval]))`, 'dropped iterations/s'),
  ], 16, 6, 8, 7, 'short'),

  row('Spring API', 13),
  timeseries('HTTP Request Rate by URI', [target('sum by (uri) (rate(http_server_requests_seconds_count{uri=~"$uri"}[$__rate_interval]))', '{{uri}}')], 0, 14, 8, 7, 'reqps'),
  timeseries('HTTP p95 by URI', [target('histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{uri=~"$uri"}[$__rate_interval])))', '{{uri}}')], 8, 14, 8, 7, 's'),
  timeseries('HTTP Errors by Status', [target('sum by (status) (rate(http_server_requests_seconds_count{status!~"2.."}[$__rate_interval]))', '{{status}}')], 16, 14, 8, 7, 'reqps'),

  row('Hikari Pool', 21),
  timeseries('Active vs Max Connections', [
    target('hikaricp_connections_active', 'active'),
    target('hikaricp_connections_max', 'max'),
  ], 0, 22, 8, 7, 'short'),
  timeseries('Pending Threads', [target('hikaricp_connections_pending', 'pending')], 8, 22, 8, 7, 'short'),
  timeseries('Idle Connections', [target('hikaricp_connections_idle', 'idle')], 16, 22, 8, 7, 'short'),

  row('PostgreSQL Activity', 29),
  timeseries('Active Sessions', [target('sum(pg_stat_activity_count{state="active"})', 'active')], 0, 30, 8, 7, 'short'),
  timeseries('Locks by Mode', [target('sum by (mode) (pg_locks_count)', '{{mode}}')], 8, 30, 8, 7, 'short'),
  timeseries('Commit vs Rollback Rate', [
    target('sum(rate(pg_stat_database_xact_commit[$__rate_interval]))', 'commit/s'),
    target('sum(rate(pg_stat_database_xact_rollback[$__rate_interval]))', 'rollback/s'),
  ], 16, 30, 8, 7, 'ops'),

  row('Table Access', 37),
  timeseries('Seq Scan by Table', [target('sum by (relname) (rate(pg_stat_user_tables_seq_scan{relname=~"$table"}[$__rate_interval]))', '{{relname}}')], 0, 38, 8, 7, 'ops'),
  timeseries('Index Scan by Table', [target('sum by (relname) (rate(pg_stat_user_tables_idx_scan{relname=~"$table"}[$__rate_interval]))', '{{relname}}')], 8, 38, 8, 7, 'ops'),
  table('Seq Tuples Read Top N', [target('topk(10, sum by (relname) (increase(pg_stat_user_tables_seq_tup_read[$__range])))', '{{relname}}')], 16, 38, 8, 7),

  row('Phase 1 Baseline Focus', 45, true),
  row('Phase 2 Index Focus', 46, true),
  row('Phase 3 N+1 Focus', 47, true),
  row('Phase 7 Pagination Focus', 48, true),
];

const dashboard = {
  annotations: { list: [] },
  editable: true,
  fiscalYearStartMonth: 0,
  graphTooltip: 1,
  id: null,
  links: [],
  liveNow: false,
  panels,
  refresh: '10s',
  schemaVersion: 39,
  tags: ['db-lab', 'ecommerce', 'observability'],
  templating: {
    list: [
      {
        name: 'phase',
        type: 'query',
        datasource,
        query: 'label_values(k6_http_reqs_total, phase)',
        current: { selected: false, text: 'phase-01', value: 'phase-01' },
        refresh: 1,
      },
      {
        name: 'scenario',
        type: 'query',
        datasource,
        query: 'label_values(k6_http_reqs_total{phase="$phase"}, scenario)',
        current: { selected: false, text: 'orders', value: 'orders' },
        refresh: 1,
      },
      {
        name: 'preset',
        type: 'query',
        datasource,
        query: 'label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario"}, preset)',
        current: { selected: false, text: 'baseline', value: 'baseline' },
        refresh: 1,
      },
      {
        name: 'pool',
        type: 'query',
        datasource,
        query: 'label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario", preset="$preset"}, pool)',
        current: { selected: false, text: 'pool10', value: 'pool10' },
        refresh: 1,
      },
      {
        name: 'uri',
        type: 'query',
        datasource,
        query: 'label_values(http_server_requests_seconds_count, uri)',
        current: { selected: true, text: '.*', value: '.*' },
        includeAll: true,
        allValue: '.*',
        refresh: 1,
      },
      {
        name: 'table',
        type: 'query',
        datasource,
        query: 'label_values(pg_stat_user_tables_seq_scan, relname)',
        current: { selected: true, text: '.*', value: '.*' },
        includeAll: true,
        allValue: '.*',
        refresh: 1,
      },
    ],
  },
  time: { from: 'now-30m', to: 'now' },
  timepicker: {},
  timezone: 'browser',
  title: 'DB Lab Overview',
  uid: 'db-lab-overview',
  version: 1,
  weekStart: '',
};

mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, `${JSON.stringify(dashboard, null, 2)}\n`);
console.log(`Generated ${outPath}`);
```

- [ ] **Step 2: Generate dashboard JSON**

Run:

```bash
rtk proxy node scripts/generate-db-lab-dashboard.mjs
```

Expected: prints `Generated /Users/kkh/Desktop/db-project/docker/grafana/dashboards/db-lab-overview.json`.

- [ ] **Step 3: Run verification**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: still FAIL because docs are not complete yet. The failure must no longer mention dashboard JSON.

- [ ] **Step 4: Commit**

```bash
git add scripts/generate-db-lab-dashboard.mjs docker/grafana/dashboards/db-lab-overview.json
git commit -m "feat: add db lab grafana dashboard"
```

---

### Task 5: Update Observability Documentation

**Files:**
- Modify: `docs/guides/grafana-observability.md`
- Modify: `docs/guides/k6-load-testing.md`
- Modify: `docs/guides/environment.md`
- Test: `scripts/verify-observability.mjs`

- [ ] **Step 1: Update `docs/guides/grafana-observability.md`**

Add this section after the opening paragraph:

```markdown
## DB Lab Overview

Grafana provisions the shared dashboard from `docker/grafana/dashboards/db-lab-overview.json`.
The dashboard spec is [DB Lab Grafana Dashboard Spec](./db-lab-grafana-dashboard-spec.md).

Use `DB Lab Overview` for common Phase Evidence:

- Run Summary: values to copy into `BASELINE.md` or phase evidence notes.
- k6 Load: actual RPS, p50/p95/p99, failed requests, dropped iterations.
- Spring API: URI-level request rate, p95, and non-2xx responses.
- Hikari Pool: active, max, idle, pending connections.
- PostgreSQL Activity: active sessions, locks, commits, rollbacks.
- Table Access: seq scan, index scan, and tuple read hot spots.

k6 metrics must be filtered by `phase`, `scenario`, `preset`, and `pool`.
Do not add `userId`, `categoryId`, `page`, SQL text, or request-specific IDs as Prometheus labels.
```

- [ ] **Step 2: Update `docs/guides/k6-load-testing.md`**

Replace the Prometheus examples with:

````markdown
Grafana에서 k6 지표까지 함께 보려면 `prometheus` 모드를 사용한다.
`PHASE`와 `POOL`은 Grafana dashboard filter에 들어가는 낮은 cardinality label이다.

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh products stress-100 prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh points points-page500 prometheus
```

Label policy:

| Label | Example | Meaning |
|---|---|---|
| `phase` | `phase-01` | Learning Phase |
| `scenario` | `orders` | k6 scenario name |
| `preset` | `baseline` | k6 preset name |
| `pool` | `pool10` | Spring Hikari profile |
````

- [ ] **Step 3: Update `docs/guides/environment.md`**

Add this paragraph after the Services table:

```markdown
Grafana provisions the `DB Lab Overview` dashboard under the `DB Lab` folder.
After `docker compose up -d`, open `http://localhost:3000`, sign in with `admin/admin`, and select `DB Lab / DB Lab Overview`.
The dashboard is empty until Spring Boot is running and at least one k6 scenario has been executed with `prometheus` mode.
```

- [ ] **Step 4: Run verification**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: PASS with `Observability configuration verified.`

- [ ] **Step 5: Commit**

```bash
git add docs/guides/grafana-observability.md docs/guides/k6-load-testing.md docs/guides/environment.md
git commit -m "docs: document db lab grafana workflow"
```

---

### Task 6: Runtime Verification

**Files:**
- No source changes unless verification exposes a real mismatch.

- [ ] **Step 1: Validate generated JSON**

Run:

```bash
rtk proxy node -e "JSON.parse(require('node:fs').readFileSync('docker/grafana/dashboards/db-lab-overview.json','utf8')); console.log('dashboard json ok')"
```

Expected: `dashboard json ok`.

- [ ] **Step 2: Start infrastructure**

Run:

```bash
rtk docker compose up -d
rtk docker compose ps
```

Expected: `postgres`, `postgres_exporter`, `prometheus`, and `grafana` are `Up`.

- [ ] **Step 3: Verify Grafana provisioning API**

Run:

```bash
rtk curl -s -u admin:admin http://localhost:3000/api/datasources
rtk curl -s -u admin:admin 'http://localhost:3000/api/search?query=DB%20Lab%20Overview'
```

Expected:

- datasource list contains `"uid":"prometheus"`.
- search result contains `"title":"DB Lab Overview"`.

- [ ] **Step 4: Verify Prometheus targets**

Run:

```bash
rtk curl -s http://localhost:9090/api/v1/targets
```

Expected:

- `postgres_exporter:9187` target is `up`.
- `host.docker.internal:8080` may be `down` if Spring Boot is not running; that is acceptable before API server startup.

- [ ] **Step 5: Run final repository verification**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: PASS with `Observability configuration verified.`

- [ ] **Step 6: Commit verification-only fixes if needed**

Only run this if previous steps required a source change:

```bash
git add <changed-files>
git commit -m "fix: align grafana dashboard provisioning"
```

---

## Self-Review

### Spec Coverage

| Spec Requirement | Plan Task |
|---|---|
| One shared `DB Lab Overview` dashboard | Task 4 |
| Phase rows instead of per-phase dashboards | Task 4 |
| k6 labels `phase`, `scenario`, `preset`, `pool` | Task 2 |
| No high-cardinality labels | Task 2 |
| Stable Prometheus datasource UID | Task 3 |
| Spring p95 dashboard support | Task 3 |
| Dashboard docs and usage workflow | Task 5 |
| Runtime provisioning verification | Task 6 |

### Placeholder Scan

This plan avoids unfinished placeholder tasks. Each changed file has exact content or exact insertion text.

### Type And Name Consistency

The dashboard, verification script, and docs use the same names:

- Dashboard title: `DB Lab Overview`
- Dashboard UID: `db-lab-overview`
- Datasource UID: `prometheus`
- k6 labels: `phase`, `scenario`, `preset`, `pool`
- k6 request names: `GET /api/orders`, `GET /api/products`, `GET /api/points`
