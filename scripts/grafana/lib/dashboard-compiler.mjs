import { resolveRepoPath, loadYaml } from './yaml-loader.mjs';
import { loadQueryRegistry, getQuery } from './query-registry.mjs';
import { createPanelState, buildPanel, buildRowPanel } from './grafana-builder.mjs';
import { createRowLayoutState, panelGrid, rowPanelGrid } from './layout.mjs';

const DASHBOARD_YAML_PATH = 'scripts/grafana/dashboards/db-lab-overview.yml';
const ROWS_PATH_PREFIX = 'scripts/grafana/rows';

const annotations = {
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
};

function isRecord(value) {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function normalizePanelQueries(panel, queryRegistry) {
  const panelTitle = panel.title ?? 'unknown';
  const queryDefinitions = [];

  if (panel.query !== undefined && panel.queries !== undefined) {
    throw new Error(`Panel ${panelTitle} must not define both query and queries`);
  }

  if (panel.query !== undefined) {
    queryDefinitions.push({
      alias: panel.query,
      legendFormat: panel.legend,
      query: getQuery(queryRegistry, panel.query, panelTitle),
      zeroWhenNoData: queryRegistry[panel.query]?.zeroWhenNoData,
    });
    return queryDefinitions;
  }

  if (!Array.isArray(panel.queries)) {
    throw new Error(`Panel ${panelTitle} must define query or queries`);
  }

  for (const item of panel.queries) {
    const alias = typeof item === 'string' ? item : item?.alias;
    if (!alias) {
      throw new Error(`Panel ${panelTitle} has a query item without alias`);
    }

    const query = getQuery(queryRegistry, alias, panelTitle);
    const legendFormat = typeof item === 'string' ? undefined : item?.legend;
    queryDefinitions.push({
      alias,
      legendFormat,
      legend: item?.legend,
      query,
      zeroWhenNoData: query.zeroWhenNoData,
    });
  }

  return queryDefinitions;
}

function normalizePanelSpec(panel, queryRegistry) {
  const normalized = {
    ...panel,
    type: panel.type,
    title: panel.title,
    queries: normalizePanelQueries(panel, queryRegistry),
  };

  if (!normalized.type || !normalized.title) {
    throw new Error('Panel definition requires type and title');
  }

  return normalized;
}

function compileDashboardVariables(variables) {
  const list = [];

  for (const [name, config] of Object.entries(variables || {})) {
    if (!isRecord(config) || typeof config.label !== 'string' || typeof config.query !== 'string') {
      throw new Error(`Variable ${name} must define label and query`);
    }

    const isAll = Boolean(config.includeAll);
    const variableConfig = {
      current: {
        selected: isAll,
        text: config.default,
        value: config.default,
      },
      datasource: {
        type: 'prometheus',
        uid: 'prometheus',
      },
      definition: config.query,
      includeAll: isAll,
      label: config.label,
      name,
      options: [],
      query: {
        query: config.query,
        refId: 'PrometheusVariableQueryEditor-VariableQuery',
      },
      refresh: 1,
      sort: 1,
      type: 'query',
    };

    if (isAll) {
      variableConfig.multi = Boolean(config.multi);
    }

    if (typeof config.multi === 'boolean') {
      variableConfig.multi = Boolean(config.multi);
    }

    list.push(variableConfig);
  }

  return list;
}

function resolveRowConfig(rowRef) {
  const rowFilePath = `${ROWS_PATH_PREFIX}/${rowRef}.yml`;
  return loadYaml(resolveRepoPath(rowFilePath));
}

function compilePanelsInRow({ panels, panelState, queryRegistry, rowStartY, currentPanels }) {
  const layoutState = createRowLayoutState();
  const rowGridStart = rowStartY + 1;
  let rowBottomOffset = 0;

  const compiled = [];

  for (const panel of panels) {
    const normalizedPanel = normalizePanelSpec(panel, queryRegistry);
    const layout = panelGrid(normalizedPanel, rowGridStart, layoutState);
    const built = buildPanel(panelState, normalizedPanel, layout.grid);
    rowBottomOffset = Math.max(rowBottomOffset, layout.yOffset + layout.h);
    compiled.push(built);
  }

  return { panels: compiled, nextRowY: rowGridStart + rowBottomOffset };
}

function compileSingleRow({ rowConfig, panelState, queryRegistry, currentY, currentPanels }) {
  const rowPanels = rowConfig.panels || [];
  if (!Array.isArray(rowPanels)) {
    throw new Error(`Row ${rowConfig.title || rowConfig.id} must include panels`);
  }

  const panel = buildRowPanel(panelState, rowConfig.title, rowPanelGrid(currentY));
  currentPanels.push(panel);

  const compiled = compilePanelsInRow({
    panels: rowPanels,
    panelState,
    queryRegistry,
    rowStartY: currentY,
    currentPanels,
  });

  return {
    panels: compiled.panels,
    nextY: compiled.nextRowY,
  };
}

function compileRowConfig(rowRef, panelState, queryRegistry, currentY, currentPanels) {
  const rowConfig = resolveRowConfig(rowRef);
  const nextRows = [];

  if (Array.isArray(rowConfig.rows)) {
    let nextY = currentY;
    for (const nested of rowConfig.rows) {
      const compiled = compileSingleRow({
        rowConfig: nested,
        panelState,
        queryRegistry,
        currentY: nextY,
        currentPanels,
      });

      currentPanels.push(...compiled.panels);
      nextY = compiled.nextY;
    }
    return { nextY, panels: [] };
  }

  const compiled = compileSingleRow({
    rowConfig,
    panelState,
    queryRegistry,
    currentY,
    currentPanels,
  });
  currentPanels.push(...compiled.panels);

  return { nextY: compiled.nextY, panels: [] };
}

export function compileDashboard(dashboardYamlPath = DASHBOARD_YAML_PATH) {
  const dashboard = loadYaml(resolveRepoPath(dashboardYamlPath));

  if (!dashboard || !dashboard.uid || !dashboard.title) {
    throw new Error(`Invalid dashboard YAML at ${dashboardYamlPath}`);
  }

  const queryRegistry = loadQueryRegistry();
  const panelState = createPanelState();
  const compiledPanels = [];
  let currentY = 0;

  for (const rowRef of dashboard.rows || []) {
    const compiled = compileRowConfig(rowRef, panelState, queryRegistry, currentY, compiledPanels);

    compiledPanels.push(...compiled.panels);
    currentY = compiled.nextY;

    if (compiled.panels.length === 0 && currentY === currentY) {
      // no-op
    }
  }

  return {
    dashboard: {
      annotations,
      editable: true,
      fiscalYearStartMonth: 0,
      graphTooltip: 0,
      id: null,
      links: [],
      panels: compiledPanels,
      refresh: '10s',
      schemaVersion: 39,
      tags: dashboard.tags || [],
      templating: {
        list: compileDashboardVariables(dashboard.variables),
      },
      time: {
        from: 'now-30m',
        to: 'now',
      },
      timepicker: {},
      timezone: 'browser',
      title: dashboard.title,
      uid: dashboard.uid,
      version: 1,
      weekStart: '',
    },
    output: resolveRepoPath(dashboard.output),
  };
}
