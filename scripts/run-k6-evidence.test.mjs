import assert from 'node:assert/strict';
import test from 'node:test';

import {
  buildMeasurement,
  buildEvidencePaths,
  buildK6RunEnv,
  buildTarget,
  classifyK6ExitStatus,
  shouldContinueAfterK6Exit,
} from './run-k6-evidence.mjs';

test('classifies k6 threshold failures as measurement results', () => {
  assert.equal(classifyK6ExitStatus(0), 'passed');
  assert.equal(classifyK6ExitStatus(99), 'threshold_failed');
  assert.equal(classifyK6ExitStatus(1), 'execution_failed');
});

test('continues evidence capture after k6 threshold failure only', () => {
  assert.equal(shouldContinueAfterK6Exit(0), true);
  assert.equal(shouldContinueAfterK6Exit(99), true);
  assert.equal(shouldContinueAfterK6Exit(1), false);
});

test('builds k6 evidence paths including summary json and exit status file', () => {
  const paths = buildEvidencePaths({
    phase: 'phase-01',
    scenario: 'orders',
    preset: 'baseline',
    pool: 'pool10',
    condition: 'pool10-baseline',
  });

  assert.equal(paths.summaryJsonFile, 'docs/evidence/phase-01/orders/pool10-baseline/k6-summary.json');
  assert.equal(paths.measurementFile, 'docs/evidence/phase-01/orders/pool10-baseline/measurement.json');
  assert.equal(paths.runWindowFile, 'docs/evidence/phase-01/orders/pool10-baseline/run-window.json');
  assert.equal(paths.exitStatusFile, 'docs/evidence/phase-01/orders/pool10-baseline/k6-exit-status.txt');
});

test('forces run window and exit status evidence for k6 runs', () => {
  const env = buildK6RunEnv({
    phase: 'phase-01',
    pool: 'pool10',
    paths: {
      summaryJsonFile: 'docs/evidence/phase-01/orders/pool10-baseline/k6-summary.json',
      runWindowFile: 'docs/evidence/phase-01/orders/pool10-baseline/run-window.json',
      exitStatusFile: 'docs/evidence/phase-01/orders/pool10-baseline/k6-exit-status.txt',
    },
  });

  assert.equal(env.PHASE, 'phase-01');
  assert.equal(env.POOL, 'pool10');
  assert.equal(env.K6_TAIL_ONLY, '0');
  assert.equal(env.K6_SUMMARY_JSON_FILE, 'docs/evidence/phase-01/orders/pool10-baseline/k6-summary.json');
  assert.equal(env.K6_RUN_WINDOW_FILE, 'docs/evidence/phase-01/orders/pool10-baseline/run-window.json');
  assert.equal(env.K6_EXIT_STATUS_FILE, 'docs/evidence/phase-01/orders/pool10-baseline/k6-exit-status.txt');
  assert.equal(env.K6_WRITE_RUN_WINDOW_ON_FAILURE, '1');
});

test('builds Phase 1 target metadata from scenario and preset data', () => {
  assert.deepEqual(
    buildTarget({
      scenario: 'orders',
      preset: 'baseline',
      presetData: { userStart: 1, userEnd: 1000 },
      env: {},
    }),
    {
      method: 'GET',
      endpoint: '/api/orders',
      queryParams: {
        userId: '1..1000',
        strategy: 'lazy',
      },
      strategy: 'lazy',
    },
  );

  assert.deepEqual(
    buildTarget({
      scenario: 'points',
      preset: 'phase1-points-page500',
      presetData: { userStart: 1, userEnd: 1000, page: 500, size: 20 },
      env: {},
    }),
    {
      method: 'GET',
      endpoint: '/api/points',
      queryParams: {
        userId: '1..1000',
        size: '20',
        page: '500',
      },
      strategy: 'phase1-points-page500',
    },
  );
});

test('builds measurement manifest for threshold failed k6 evidence', () => {
  const paths = buildEvidencePaths({
    phase: 'phase-01',
    scenario: 'orders',
    preset: 'baseline',
    pool: 'pool10',
    condition: 'orders-baseline',
  });
  const measurement = buildMeasurement({
    args: {
      phase: 'phase-01',
      scenario: 'orders',
      preset: 'baseline',
      pool: 'pool10',
      mode: 'prometheus',
      condition: 'orders-baseline',
    },
    paths,
    presetData: {
      rate: 50,
      duration: '5m',
      timeout: '5s',
      preAllocatedVUs: 100,
      maxVUs: 300,
      userStart: 1,
      userEnd: 1000,
    },
    exitStatus: 99,
    env: {},
    git: {
      commitSha: 'abc123',
      dirty: true,
    },
  });

  assert.equal(measurement.schemaVersion, 1);
  assert.equal(measurement.condition, 'orders-baseline');
  assert.equal(measurement.execution.exitStatus, 99);
  assert.equal(measurement.execution.resultStatus, 'threshold_failed');
  assert.equal(measurement.evidence.exitStatus, 99);
  assert.equal(measurement.evidence.files.summaryJson, 'docs/evidence/phase-01/orders/orders-baseline/k6-summary.json');
  assert.equal(measurement.workload.rate, 50);
  assert.equal(measurement.workload.duration, '5m');
  assert.equal(measurement.target.endpoint, '/api/orders');
  assert.equal(measurement.git.commitSha, 'abc123');
  assert.equal(measurement.git.dirty, true);
});
