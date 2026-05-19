export const PHASE_FOCUS_ROWS = {
  'phase-01': 'Phase 1 Baseline Focus',
  'phase-02': 'Phase 2 Index Focus',
  'phase-03': 'Phase 3 N+1 Focus',
  'phase-07': 'Phase 7 Pagination Focus',
};

const DEFAULT_DASHBOARD_URL = 'http://localhost:3000/d/db-lab-overview/db-lab-overview';

export const DEFAULT_DASHBOARD_VARS = {
  phase: 'phase-02',
  scenario: 'products',
  preset: 'baseline',
  pool: 'pool10',
  uri: '$__all',
  table: '$__all',
};

export function buildCapturePlan({ scrollHeight, clientHeight, step = clientHeight }) {
  if (!Number.isFinite(scrollHeight) || scrollHeight <= 0) {
    throw new Error('scrollHeight must be a positive number');
  }
  if (!Number.isFinite(clientHeight) || clientHeight <= 0) {
    throw new Error('clientHeight must be a positive number');
  }
  if (!Number.isFinite(step) || step <= 0) {
    throw new Error('step must be a positive number');
  }

  const maxScrollTop = Math.max(0, scrollHeight - clientHeight);
  if (maxScrollTop === 0) {
    return [0];
  }

  const offsets = [];
  for (let offset = 0; offset < maxScrollTop; offset += step) {
    offsets.push(Math.round(offset));
  }

  if (offsets.at(-1) !== maxScrollTop) {
    offsets.push(maxScrollTop);
  }

  return [...new Set(offsets)];
}

export function getPhaseFromUrl(url) {
  return new URL(url).searchParams.get('var-phase');
}

export function getDashboardVarsFromUrl(url) {
  const params = new URL(url).searchParams;

  return {
    phase: params.get('var-phase') ?? DEFAULT_DASHBOARD_VARS.phase,
    scenario: params.get('var-scenario') ?? DEFAULT_DASHBOARD_VARS.scenario,
    preset: params.get('var-preset') ?? DEFAULT_DASHBOARD_VARS.preset,
    pool: params.get('var-pool') ?? DEFAULT_DASHBOARD_VARS.pool,
    uri: params.get('var-uri') ?? DEFAULT_DASHBOARD_VARS.uri,
    table: params.get('var-table') ?? DEFAULT_DASHBOARD_VARS.table,
  };
}

export function buildGrafanaDashboardUrl({
  phase = DEFAULT_DASHBOARD_VARS.phase,
  scenario = DEFAULT_DASHBOARD_VARS.scenario,
  preset = DEFAULT_DASHBOARD_VARS.preset,
  pool = DEFAULT_DASHBOARD_VARS.pool,
  uri = DEFAULT_DASHBOARD_VARS.uri,
  table = DEFAULT_DASHBOARD_VARS.table,
  from = 'now-30m',
  to = 'now',
  refresh = '10s',
} = {}) {
  const url = new URL(DEFAULT_DASHBOARD_URL);
  url.searchParams.set('orgId', '1');
  url.searchParams.set('from', from);
  url.searchParams.set('to', to);
  url.searchParams.set('timezone', 'browser');
  url.searchParams.set('var-phase', phase);
  url.searchParams.set('var-scenario', scenario);
  url.searchParams.set('var-preset', preset);
  url.searchParams.set('var-pool', pool);
  url.searchParams.set('var-uri', uri);
  url.searchParams.set('var-table', table);
  url.searchParams.set('refresh', refresh);
  return url.toString();
}

export function buildEvidenceOutputPath({
  phase,
  scenario,
  preset,
  pool,
}) {
  return `docs/evidence/${phase}/grafana-screenshots/${scenario}-${preset}-${pool}.png`;
}

export function getFocusRowTitleForPhase(phase) {
  const title = PHASE_FOCUS_ROWS[phase];
  if (!title) {
    throw new Error(`Unsupported phase for focus row capture: ${phase}`);
  }

  return title;
}

export function buildPasteSegments({ offsets, clientHeight, scrollHeight }) {
  let coveredUntil = 0;

  return [...offsets]
    .sort((a, b) => a - b)
    .flatMap((offset) => {
      const captureEnd = Math.min(offset + clientHeight, scrollHeight);
      const sourceY = Math.max(0, coveredUntil - offset);
      const pasteY = offset + sourceY;
      const height = captureEnd - pasteY;
      coveredUntil = Math.max(coveredUntil, captureEnd);

      if (height <= 0) {
        return [];
      }

      return [{ offset, sourceY, height, pasteY }];
    });
}
