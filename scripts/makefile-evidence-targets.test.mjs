import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)));
const MAKE_ENV_KEYS = [
  'PHASE',
  'SCENARIO',
  'PRESET',
  'K6_PRESET',
  'GRAFANA_PRESET',
  'CONDITION',
  'FILE',
  'SEED_PRESET',
  'SEED_STATE_OUTPUT',
  'SEED_STATE_SQL',
];

function make(args) {
  const env = { ...process.env };
  for (const key of MAKE_ENV_KEYS) {
    delete env[key];
  }

  return spawnSync('make', args, {
    cwd: ROOT,
    encoding: 'utf8',
    env,
  });
}

test('k6 evidence requires explicit phase, scenario, condition, and preset', () => {
  const result = make(['-n', 'k6-evidence']);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /PHASE is required/);
});

test('k6 evidence rejects missing preset instead of using a phase-specific default', () => {
  const result = make([
    '-n',
    'k6-evidence',
    'PHASE=phase-01',
    'SCENARIO=orders',
    'CONDITION=orders-baseline',
  ]);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /K6_PRESET is required/);
});

test('k6 evidence uses explicitly provided measurement variables', () => {
  const result = make([
    '-n',
    'k6-evidence',
    'PHASE=phase-01',
    'SCENARIO=orders',
    'PRESET=baseline',
    'CONDITION=orders-baseline',
  ]);

  assert.equal(result.status, 0);
  assert.match(
    result.stdout,
    /--phase phase-01 --scenario orders --preset baseline --pool pool10 --mode prometheus --condition orders-baseline/,
  );
});

test('grafana capture requires explicit dashboard variables', () => {
  const result = make(['-n', 'grafana-capture']);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /PHASE is required/);
});

test('phase SQL requires an explicit SQL file', () => {
  const result = make(['-n', 'phase-sql']);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /FILE is required/);
});

test('seed state requires an explicit seed preset', () => {
  const result = make(['-n', 'seed-state']);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /SEED_PRESET is required/);
});

test('seed state runs seed and captures the common seed-state artifact', () => {
  const result = make(['-n', 'seed-state', 'SEED_PRESET=loadtest']);

  assert.equal(result.status, 0);
  assert.match(result.stdout, /\.\/scripts\/seed\.sh loadtest/);
  assert.match(
    result.stdout,
    /--file "scripts\/db-state\/00-seed-state\.sql" --output "docs\/evidence\/common\/seed-loadtest\/seed-state\.txt"/,
  );
});
