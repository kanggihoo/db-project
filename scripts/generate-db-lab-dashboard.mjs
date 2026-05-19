import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const outputPath = join(root, 'docker/grafana/dashboards/db-lab-overview.json');

const datasource = {
  type: 'prometheus',
  uid: 'prometheus',
};

const k6Filter = 'phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"';
const zeroWhenNoData = (expr) => `(${expr}) or vector(0)`;

let nextPanelId = 1;
let nextRefCode = 'A'.charCodeAt(0);
let y = 0;

function nextRefId() {
  const refId = String.fromCharCode(nextRefCode);
  nextRefCode += 1;
  return refId;
}

function target(expr, legendFormat = undefined) {
  return {
    datasource,
    editorMode: 'code',
    expr,
    legendFormat,
    range: true,
    refId: nextRefId(),
  };
}

function gridPos(h, w = 12) {
  const pos = { h, w, x: 0, y };
  y += h;
  return pos;
}

function row(title) {
  return {
    collapsed: false,
    gridPos: gridPos(1, 24),
    id: nextPanelId++,
    panels: [],
    title,
    type: 'row',
  };
}

function stat(title, expr, x, yPos, w = 4) {
  return {
    datasource,
    fieldConfig: {
      defaults: {
        color: { mode: 'thresholds' },
        mappings: [],
        thresholds: {
          mode: 'absolute',
          steps: [
            { color: 'green', value: null },
            { color: 'red', value: 80 },
          ],
        },
      },
      overrides: [],
    },
    gridPos: { h: 4, w, x, y: yPos },
    id: nextPanelId++,
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
    pluginVersion: '11.0.0',
    targets: [target(expr)],
    title,
    type: 'stat',
  };
}

function timeSeries(title, expr, legendFormat, x, yPos, w = 12, h = 8) {
  return {
    datasource,
    fieldConfig: {
      defaults: {
        color: { mode: 'palette-classic' },
        custom: {
          axisBorderShow: false,
          axisCenteredZero: false,
          axisColorMode: 'text',
          axisLabel: '',
          axisPlacement: 'auto',
          barAlignment: 0,
          drawStyle: 'line',
          fillOpacity: 10,
          gradientMode: 'none',
          hideFrom: { legend: false, tooltip: false, viz: false },
          insertNulls: false,
          lineInterpolation: 'linear',
          lineWidth: 1,
          pointSize: 5,
          scaleDistribution: { type: 'linear' },
          showPoints: 'never',
          spanNulls: false,
          stacking: { group: 'A', mode: 'none' },
          thresholdsStyle: { mode: 'off' },
        },
        mappings: [],
        thresholds: {
          mode: 'absolute',
          steps: [
            { color: 'green', value: null },
            { color: 'red', value: 80 },
          ],
        },
      },
      overrides: [],
    },
    gridPos: { h, w, x, y: yPos },
    id: nextPanelId++,
    options: {
      legend: {
        calcs: ['lastNotNull'],
        displayMode: 'list',
        placement: 'bottom',
        showLegend: true,
      },
      tooltip: {
        mode: 'single',
        sort: 'none',
      },
    },
    targets: [target(expr, legendFormat)],
    title,
    type: 'timeseries',
  };
}

function table(title, expr, x, yPos, w = 12, h = 8) {
  return {
    datasource,
    fieldConfig: {
      defaults: {
        color: { mode: 'thresholds' },
        custom: {
          align: 'auto',
          cellOptions: { type: 'auto' },
          inspect: false,
        },
        mappings: [],
        thresholds: {
          mode: 'absolute',
          steps: [
            { color: 'green', value: null },
            { color: 'red', value: 80 },
          ],
        },
      },
      overrides: [],
    },
    gridPos: { h, w, x, y: yPos },
    id: nextPanelId++,
    options: {
      cellHeight: 'sm',
      footer: {
        countRows: false,
        fields: '',
        reducer: ['sum'],
        show: false,
      },
      showHeader: true,
    },
    targets: [target(expr)],
    title,
    type: 'table',
  };
}

function addRowWithPanels(panels, title, builder) {
  panels.push(row(title));
  const rowY = y;
  builder(rowY, panels);
}

const panels = [];

addRowWithPanels(panels, 'Run Summary', (rowY, targetPanels) => {
  targetPanels.push(
    stat('k6 p95', `histogram_quantile(0.95, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 0, rowY),
    stat('k6 p99', `histogram_quantile(0.99, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 4, rowY),
    stat('Error Rate', `avg(k6_http_req_failed_rate{${k6Filter}})`, 8, rowY),
    stat('Actual RPS', `sum(rate(k6_http_reqs_total{${k6Filter}}[$__rate_interval]))`, 12, rowY),
    stat('Dropped Iterations', zeroWhenNoData(`sum(increase(k6_dropped_iterations_total{${k6Filter}}[$__range]))`), 16, rowY),
    stat('Hikari Pending Max', 'max_over_time(hikaricp_connections_pending[$__range])', 20, rowY),
  );
  y = rowY + 4;
});

addRowWithPanels(panels, 'k6 Load', (rowY, targetPanels) => {
  targetPanels.push(
    timeSeries('Actual RPS', `sum(rate(k6_http_reqs_total{${k6Filter}}[$__rate_interval]))`, 'rps', 0, rowY),
    timeSeries('k6 Latency p95', `histogram_quantile(0.95, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 'p95', 12, rowY),
    timeSeries('Error Rate', `avg(k6_http_req_failed_rate{${k6Filter}})`, 'failed', 0, rowY + 8),
    timeSeries('Dropped Iterations', zeroWhenNoData(`sum(rate(k6_dropped_iterations_total{${k6Filter}}[$__rate_interval]))`), 'dropped', 12, rowY + 8),
  );
  y = rowY + 16;
});

addRowWithPanels(panels, 'Spring API', (rowY, targetPanels) => {
  targetPanels.push(
    timeSeries('HTTP Request Rate by URI', 'sum by (uri) (rate(http_server_requests_seconds_count{uri=~"$uri"}[$__rate_interval]))', '{{uri}}', 0, rowY),
    timeSeries('HTTP p95 by URI', 'histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{uri=~"$uri"}[$__rate_interval])))', '{{uri}}', 12, rowY),
    timeSeries('HTTP Errors by Status', 'sum by (status) (rate(http_server_requests_seconds_count{status!~"2.."}[$__rate_interval]))', '{{status}}', 0, rowY + 8),
    table('Slowest URI', 'sort_desc(histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{uri=~"$uri"}[$__rate_interval]))))', 12, rowY + 8),
  );
  y = rowY + 16;
});

addRowWithPanels(panels, 'Hikari Pool', (rowY, targetPanels) => {
  targetPanels.push(
    timeSeries('Active Connections', 'hikaricp_connections_active', 'active', 0, rowY),
    timeSeries('Max Connections', 'hikaricp_connections_max', 'max', 12, rowY),
    timeSeries('Pending Threads', 'hikaricp_connections_pending', 'pending', 0, rowY + 8),
    timeSeries('Acquire Time', 'rate(hikaricp_connections_acquire_seconds_sum[$__rate_interval]) / rate(hikaricp_connections_acquire_seconds_count[$__rate_interval])', 'avg acquire', 12, rowY + 8),
  );
  y = rowY + 16;
});

addRowWithPanels(panels, 'PostgreSQL Activity', (rowY, targetPanels) => {
  targetPanels.push(
    timeSeries('Active Sessions', 'sum(pg_stat_activity_count{state="active"})', 'active', 0, rowY),
    timeSeries('Locks', 'sum by (mode) (pg_locks_count)', '{{mode}}', 12, rowY),
    timeSeries('Commit Rate', 'rate(pg_stat_database_xact_commit[$__rate_interval])', 'commit', 0, rowY + 8),
    timeSeries('Rollback Rate', 'rate(pg_stat_database_xact_rollback[$__rate_interval])', 'rollback', 12, rowY + 8),
  );
  y = rowY + 16;
});

addRowWithPanels(panels, 'Table Access', (rowY, targetPanels) => {
  targetPanels.push(
    timeSeries('Seq Scan by Table', 'sum by (relname) (rate(pg_stat_user_tables_seq_scan{relname=~"$table"}[$__rate_interval]))', '{{relname}}', 0, rowY),
    timeSeries('Index Scan by Table', 'sum by (relname) (rate(pg_stat_user_tables_idx_scan{relname=~"$table"}[$__rate_interval]))', '{{relname}}', 12, rowY),
    table('Seq Tuples Read Top N', 'topk(10, increase(pg_stat_user_tables_seq_tup_read[$__range]))', 0, rowY + 8),
    table('Index Tuples Fetch Top N', 'topk(10, increase(pg_stat_user_tables_idx_tup_fetch[$__range]))', 12, rowY + 8),
  );
  y = rowY + 16;
});

for (const title of ['Phase 1 Baseline Focus', 'Phase 2 Index Focus', 'Phase 3 N+1 Focus', 'Phase 7 Pagination Focus']) {
  addRowWithPanels(panels, title, (rowY, targetPanels) => {
    targetPanels.push(
      timeSeries(`${title} Latency`, `histogram_quantile(0.95, sum(rate(k6_http_req_duration_seconds{${k6Filter}}[$__rate_interval])))`, 'p95', 0, rowY),
      timeSeries(`${title} Table Scan`, 'sum by (relname) (rate(pg_stat_user_tables_seq_scan{relname=~"$table"}[$__rate_interval]))', '{{relname}}', 12, rowY),
    );
    y = rowY + 8;
  });
}

const dashboard = {
  annotations: {
    list: [
      {
        builtIn: 1,
        datasource: {
          type: 'grafana',
          uid: '-- Grafana --',
        },
        enable: true,
        hide: true,
        iconColor: 'rgba(0, 211, 255, 1)',
        name: 'Annotations & Alerts',
        type: 'dashboard',
      },
    ],
  },
  editable: true,
  fiscalYearStartMonth: 0,
  graphTooltip: 0,
  id: null,
  links: [],
  panels,
  refresh: '10s',
  schemaVersion: 39,
  tags: ['db-lab', 'ecommerce', 'observability'],
  templating: {
    list: [
      {
        current: { selected: false, text: 'phase-01', value: 'phase-01' },
        datasource,
        definition: 'label_values(k6_http_reqs_total, phase)',
        includeAll: false,
        label: 'Phase',
        name: 'phase',
        options: [],
        query: { query: 'label_values(k6_http_reqs_total, phase)', refId: 'PrometheusVariableQueryEditor-VariableQuery' },
        refresh: 1,
        sort: 1,
        type: 'query',
      },
      {
        current: { selected: false, text: 'orders', value: 'orders' },
        datasource,
        definition: 'label_values(k6_http_reqs_total{phase="$phase"}, scenario)',
        includeAll: false,
        label: 'Scenario',
        name: 'scenario',
        options: [],
        query: { query: 'label_values(k6_http_reqs_total{phase="$phase"}, scenario)', refId: 'PrometheusVariableQueryEditor-VariableQuery' },
        refresh: 1,
        sort: 1,
        type: 'query',
      },
      {
        current: { selected: false, text: 'baseline', value: 'baseline' },
        datasource,
        definition: 'label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario"}, preset)',
        includeAll: false,
        label: 'Preset',
        name: 'preset',
        options: [],
        query: { query: 'label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario"}, preset)', refId: 'PrometheusVariableQueryEditor-VariableQuery' },
        refresh: 1,
        sort: 1,
        type: 'query',
      },
      {
        current: { selected: false, text: 'pool10', value: 'pool10' },
        datasource,
        definition: 'label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario", preset="$preset"}, pool)',
        includeAll: false,
        label: 'Pool',
        name: 'pool',
        options: [],
        query: { query: 'label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario", preset="$preset"}, pool)', refId: 'PrometheusVariableQueryEditor-VariableQuery' },
        refresh: 1,
        sort: 1,
        type: 'query',
      },
      {
        current: { selected: true, text: 'All', value: '$__all' },
        datasource,
        definition: 'label_values(http_server_requests_seconds_count, uri)',
        includeAll: true,
        label: 'URI',
        multi: true,
        name: 'uri',
        options: [],
        query: { query: 'label_values(http_server_requests_seconds_count, uri)', refId: 'PrometheusVariableQueryEditor-VariableQuery' },
        refresh: 1,
        sort: 1,
        type: 'query',
      },
      {
        current: { selected: true, text: 'All', value: '$__all' },
        datasource,
        definition: 'label_values(pg_stat_user_tables_seq_scan, relname)',
        includeAll: true,
        label: 'Table',
        multi: true,
        name: 'table',
        options: [],
        query: { query: 'label_values(pg_stat_user_tables_seq_scan, relname)', refId: 'PrometheusVariableQueryEditor-VariableQuery' },
        refresh: 1,
        sort: 1,
        type: 'query',
      },
    ],
  },
  time: {
    from: 'now-30m',
    to: 'now',
  },
  timepicker: {},
  timezone: 'browser',
  title: 'DB Lab Overview',
  uid: 'db-lab-overview',
  version: 1,
  weekStart: '',
};

mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(outputPath, `${JSON.stringify(dashboard, null, 2)}\n`);
console.log(`Generated ${outputPath}`);
