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

function assertAny(content, candidates, message) {
  assert(
    candidates.some((candidate) => content.includes(candidate)),
    `${message}. Expected one of: ${candidates.join(', ')}`,
  );
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

function verifyScenario(file, scenario, requestName, defaults = {}) {
  const phase = defaults.phase || 'phase-01';
  const preset = defaults.preset || 'baseline';
  const content = read(file);
  const usesSharedLib = content.includes('loadConfig(');

  if (usesSharedLib) {
    assertAny(content, [`defaultScenario: '${scenario}'`, `defaultScenario: "${scenario}"`], `${file} must pass defaultScenario`);
    assertAny(content, [`defaultPhase: '${phase}'`, `defaultPhase: "${phase}"`], `${file} must pass defaultPhase`);
    assertAny(content, [`defaultPresetName: '${preset}'`, `defaultPresetName: "${preset}"`], `${file} must pass defaultPresetName`);
    assertAny(content, ["defaultPool: 'pool10'", 'defaultPool: "pool10"'], `${file} must pass defaultPool`);
    assertAny(
      content,
      ['buildConstantArrivalRateOptions(config, {', 'buildConstantArrivalRateOptions(config, {'],
      `${file} must use buildConstantArrivalRateOptions`,
    );
    assertAny(content, ["createRequestTags(config, '", 'createRequestTags(config, "'], `${file} must use createRequestTags`);
    assert(content.includes(`'${requestName}'`) || content.includes(`\"${requestName}\"`), `${file} must include request name '${requestName}'`);
  } else {
    for (const expected of [
      `scenario: __ENV.SCENARIO || '${scenario}'`,
      `phase: __ENV.PHASE || '${phase}'`,
      `preset: __ENV.PRESET_NAME || '${preset}'`,
      "pool: __ENV.POOL || 'pool10'",
      "systemTags: ['status', 'method', 'name', 'expected_response']",
      `name: '${requestName}'`,
      'tags: requestTags',
    ]) {
      assert(content.includes(expected), `${file} must include ${expected}`);
    }
  }

  assert(content.includes("systemTags: ['status', 'method', 'name', 'expected_response']") || content.includes('buildConstantArrivalRateOptions(config'), `${file} must include expected systemTags via options`);
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
  for (const expected of [
    '$phase',
    '$scenario',
    '$preset',
    '$pool',
    'k6_http_reqs_total',
    'k6_http_req_duration_seconds',
    'k6_iteration_duration_seconds',
    'k6_checks_rate',
    'expected_response',
    'hikaricp_connections_timeout_total',
    'process_cpu_usage',
    'jvm_memory_used_bytes',
    'jvm_gc_pause_seconds_sum',
    'pg_stat_database_numbackends',
    'pg_settings_max_connections',
    'hikaricp_connections_pending',
    'pg_stat_user_tables_seq_scan',
  ]) {
    assert(dashboardText.includes(expected), `dashboard must include ${expected}`);
  }
  const targetsWithoutFallback = dashboard.panels.flatMap((panel) =>
    (panel.targets || [])
      .filter((target) => target.expr && !target.expr.includes('or vector(0)'))
      .map((target) => `${panel.title}: ${target.expr}`),
  );
  assert(
    targetsWithoutFallback.length === 0,
    `dashboard targets must use no-data fallback: ${targetsWithoutFallback.slice(0, 3).join(', ')}`,
  );

  const positionedPanels = dashboard.panels
    .filter((panel) => panel.gridPos)
    .map((panel) => ({
      title: panel.title,
      type: panel.type,
      ...panel.gridPos,
    }));
  const overlaps = [];
  for (let i = 0; i < positionedPanels.length; i += 1) {
    for (let j = i + 1; j < positionedPanels.length; j += 1) {
      const a = positionedPanels[i];
      const b = positionedPanels[j];
      const overlapsX = a.x < b.x + b.w && b.x < a.x + a.w;
      const overlapsY = a.y < b.y + b.h && b.y < a.y + a.h;
      if (overlapsX && overlapsY) {
        overlaps.push(`${a.title} (${a.type}) overlaps ${b.title} (${b.type})`);
      }
    }
  }
  assert(overlaps.length === 0, `dashboard panels must not overlap: ${overlaps.slice(0, 3).join(', ')}`);
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
verifyScenario('k6/points-cursor-test.js', 'points-cursor', 'GET /api/points/cursor', {
  phase: 'phase-07',
  preset: 'cursor',
});
verifyGrafanaProvisioning();
verifyDashboard();
verifySpringHistograms();
verifyDocs();

console.log('Observability configuration verified.');
