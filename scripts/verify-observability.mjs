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
