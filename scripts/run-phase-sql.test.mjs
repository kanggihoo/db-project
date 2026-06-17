import assert from 'node:assert/strict';
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import {
  buildPsqlCommand,
  parseArgs,
  resolveSqlFile,
  runPhaseSql,
} from './run-phase-sql.mjs';

test('parses file and output arguments', () => {
  const args = parseArgs([
    '--file',
    'scripts/phase-01/10-products-baseline-explain.sql',
    '--output',
    'docs/evidence/phase-01/explain/products-baseline-filter.txt',
  ]);

  assert.deepEqual(args, {
    file: 'scripts/phase-01/10-products-baseline-explain.sql',
    output: 'docs/evidence/phase-01/explain/products-baseline-filter.txt',
  });
});

test('requires an explicit SQL file', () => {
  assert.throws(
    () => resolveSqlFile({ phase: 'phase-06', scenario: 'data-profile', action: 'profile' }),
    /Missing required argument: --file/,
  );
});

test('builds the Docker psql command used by all phases', () => {
  assert.deepEqual(buildPsqlCommand(), [
    'compose',
    'exec',
    '-T',
    'postgres',
    'psql',
    '-U',
    'app',
    '-d',
    'ecommerce',
    '-v',
    'ON_ERROR_STOP=1',
    '-f',
    '-',
  ]);
});

test('runs an explicit SQL file and writes stdout to output', () => {
  const root = join(tmpdir(), `phase-sql-${Date.now()}`);
  const sqlFile = join(root, 'query.sql');
  const outputFile = join(root, 'nested', 'result.txt');
  mkdirSync(root, { recursive: true });
  writeFileSync(sqlFile, 'SELECT 1;\n');

  const calls = [];
  const status = runPhaseSql(
    { file: sqlFile, output: outputFile },
    {
      readFileSync,
      mkdirSync,
      writeFileSync,
      spawnSync(command, args, options) {
        calls.push({ command, args, options });
        return { status: 0, stdout: 'ok\n', stderr: '' };
      },
      stdout: { write() {} },
      stderr: { write() {} },
    },
  );

  assert.equal(status, 0);
  assert.equal(readFileSync(outputFile, 'utf8'), 'ok\n');
  assert.equal(calls[0].command, 'docker');
  assert.deepEqual(calls[0].args, buildPsqlCommand());
  assert.equal(calls[0].options.input, 'SELECT 1;\n');
  rmSync(root, { recursive: true, force: true });
});
