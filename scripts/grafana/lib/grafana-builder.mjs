const datasource = {
  type: 'prometheus',
  uid: 'prometheus',
};

const DEFAULT_THRESHOLDS = {
  mode: 'absolute',
  steps: [
    { color: 'green', value: null },
    { color: 'red', value: 80 },
  ],
};

const DEFAULT_COMMON_COLOR = { mode: 'thresholds' };

function createState() {
  return {
    nextPanelId: 1,
    nextRefCode: 'A'.charCodeAt(0),
  };
}

function nextRefId(panelState) {
  const ref = String.fromCharCode(panelState.nextRefCode);
  panelState.nextRefCode += 1;
  return ref;
}

function nextPanelId(panelState) {
  return panelState.nextPanelId++;
}

function applyNoData(expr, shouldUseFallback) {
  if (!shouldUseFallback || expr.includes('or vector(0)')) {
    return expr;
  }
  return `(${expr}) or vector(0)`;
}

function buildTarget(query, legendFormat, panelState, zeroWhenNoData) {
  return {
    datasource,
    editorMode: 'code',
    expr: applyNoData(query.expr, Boolean(zeroWhenNoData)),
    legendFormat,
    range: true,
    refId: nextRefId(panelState),
  };
}

function buildStatDefaults(unit) {
  return {
    color: DEFAULT_COMMON_COLOR,
    mappings: [],
    ...(unit ? { unit } : {}),
    thresholds: DEFAULT_THRESHOLDS,
  };
}

function buildTimeseriesDefaults() {
  return {
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
      hideFrom: {
        legend: false,
        tooltip: false,
        viz: false,
      },
      insertNulls: false,
      lineInterpolation: 'linear',
      lineWidth: 1,
      pointSize: 5,
      scaleDistribution: {
        type: 'linear',
      },
      showPoints: 'never',
      spanNulls: false,
      stacking: {
        group: 'A',
        mode: 'none',
      },
      thresholdsStyle: { mode: 'off' },
    },
    mappings: [],
    thresholds: DEFAULT_THRESHOLDS,
  };
}

function buildTableDefaults() {
  return {
    color: DEFAULT_COMMON_COLOR,
    custom: {
      align: 'auto',
      cellOptions: { type: 'auto' },
      inspect: false,
    },
    mappings: [],
    thresholds: DEFAULT_THRESHOLDS,
  };
}

function buildTargets(panelState, queryRefs, unit) {
  return queryRefs.map((queryRef) => {
    return buildTarget(
      queryRef.query,
      queryRef.legendFormat ?? queryRef.legend,
      panelState,
      queryRef.zeroWhenNoData,
    );
  });
}

function buildOverrides(overrides) {
  return overrides || [];
}

function normalizePanelType(type) {
  return String(type || '').toLowerCase();
}

export function buildRowPanel(rowState, title, gridPos) {
  return {
    collapsed: false,
    gridPos,
    id: nextPanelId(rowState),
    panels: [],
    title,
    type: 'row',
  };
}

export function buildStatPanel(panelState, panelConfig, gridPos) {
  const options = {
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
  };

  return {
    datasource,
    fieldConfig: {
      defaults: buildStatDefaults(panelConfig.unit),
      overrides: [],
    },
    gridPos,
    id: nextPanelId(panelState),
    options,
    pluginVersion: '11.0.0',
    targets: buildTargets(panelState, panelConfig.queries),
    title: panelConfig.title,
    type: 'stat',
  };
}

export function buildTimeseriesPanel(panelState, panelConfig, gridPos) {
  const defaults = {
    calcs: ['lastNotNull'],
    displayMode: 'list',
    placement: 'bottom',
    showLegend: true,
  };

  return {
    datasource,
    fieldConfig: {
      defaults: buildTimeseriesDefaults(),
      overrides: buildOverrides(panelConfig.overrides),
    },
    gridPos,
    id: nextPanelId(panelState),
    options: {
      legend: defaults,
      tooltip: {
        mode: panelConfig.tooltipMode || 'single',
        sort: 'none',
      },
    },
    targets: buildTargets(panelState, panelConfig.queries),
    title: panelConfig.title,
    type: 'timeseries',
  };
}

export function buildTablePanel(panelState, panelConfig, gridPos) {
  return {
    datasource,
    fieldConfig: {
      defaults: buildTableDefaults(),
      overrides: buildOverrides(panelConfig.overrides),
    },
    gridPos,
    id: nextPanelId(panelState),
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
    targets: buildTargets(panelState, panelConfig.queries),
    title: panelConfig.title,
    type: 'table',
  };
}

export function buildPanel(panelState, panelSpec, gridPos) {
  const panelType = normalizePanelType(panelSpec.type);
  if (panelType === 'stat') {
    return buildStatPanel(panelState, panelSpec, gridPos);
  }
  if (panelType === 'timeseries') {
    return buildTimeseriesPanel(panelState, panelSpec, gridPos);
  }
  if (panelType === 'table') {
    return buildTablePanel(panelState, panelSpec, gridPos);
  }

  throw new Error(`Unsupported panel type: ${panelSpec.type}`);
}

export function createPanelState() {
  return createState();
}
