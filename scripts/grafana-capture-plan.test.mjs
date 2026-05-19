import test from 'node:test';
import assert from 'node:assert/strict';

import {
  buildCapturePlan,
  buildPasteSegments,
  buildEvidenceOutputPath,
  buildGrafanaDashboardUrl,
  getFocusRowTitleForPhase,
  getPhaseFromUrl,
} from './grafana-capture-plan.mjs';

test('buildCapturePlan includes max scroll offset when the final viewport would otherwise miss the end', () => {
  assert.deepEqual(
    buildCapturePlan({ scrollHeight: 4797, clientHeight: 937, step: 937 }),
    [0, 937, 1874, 2811, 3748, 3860],
  );
});

test('buildCapturePlan returns a single top capture when content fits in one viewport', () => {
  assert.deepEqual(
    buildCapturePlan({ scrollHeight: 900, clientHeight: 937, step: 937 }),
    [0],
  );
});

test('buildPasteSegments removes only the duplicated top area from overlapping captures', () => {
  assert.deepEqual(
    buildPasteSegments({
      offsets: [0, 937, 1874, 2811, 3748, 3860],
      clientHeight: 937,
      scrollHeight: 4797,
    }),
    [
      { offset: 0, sourceY: 0, height: 937, pasteY: 0 },
      { offset: 937, sourceY: 0, height: 937, pasteY: 937 },
      { offset: 1874, sourceY: 0, height: 937, pasteY: 1874 },
      { offset: 2811, sourceY: 0, height: 937, pasteY: 2811 },
      { offset: 3748, sourceY: 0, height: 937, pasteY: 3748 },
      { offset: 3860, sourceY: 825, height: 112, pasteY: 4685 },
    ],
  );
});

test('getPhaseFromUrl reads the selected Grafana phase variable', () => {
  assert.equal(
    getPhaseFromUrl('http://localhost:3000/d/db?orgId=1&var-phase=phase-03&var-scenario=orders'),
    'phase-03',
  );
});

test('getFocusRowTitleForPhase maps phase ids to dashboard focus rows', () => {
  assert.equal(getFocusRowTitleForPhase('phase-03'), 'Phase 3 N+1 Focus');
});

test('getFocusRowTitleForPhase rejects unsupported phase ids', () => {
  assert.throws(() => getFocusRowTitleForPhase('phase-99'), /Unsupported phase/);
});

test('buildEvidenceOutputPath stores screenshots under the selected phase', () => {
  assert.equal(
    buildEvidenceOutputPath({
      phase: 'phase-03',
      scenario: 'orders',
      preset: 'baseline',
      pool: 'pool10',
    }),
    'docs/evidence/phase-03/grafana-screenshots/orders-baseline-pool10.png',
  );
});

test('buildGrafanaDashboardUrl applies selected dashboard variables', () => {
  const url = new URL(
    buildGrafanaDashboardUrl({
      phase: 'phase-03',
      scenario: 'orders',
      preset: 'baseline',
      pool: 'pool20',
      uri: '/api/orders',
      table: 'orders',
    }),
  );

  assert.equal(url.searchParams.get('var-phase'), 'phase-03');
  assert.equal(url.searchParams.get('var-scenario'), 'orders');
  assert.equal(url.searchParams.get('var-preset'), 'baseline');
  assert.equal(url.searchParams.get('var-pool'), 'pool20');
  assert.equal(url.searchParams.get('var-uri'), '/api/orders');
  assert.equal(url.searchParams.get('var-table'), 'orders');
});
