import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { spawnSync } from 'node:child_process';

const PHASE_06_SCRIPTS = {
  'data-profile:profile': 'scripts/phase-06/00-data-profile.sql',
  'review-aggregate:baseline:prepare': 'scripts/phase-06/10-review-baseline-prepare.sql',
  'review-aggregate:baseline:explain': 'scripts/phase-06/11-review-baseline-explain.sql',
  'review-aggregate:naive-index:prepare': 'scripts/phase-06/12-review-naive-prepare.sql',
  'review-aggregate:naive-index:explain': 'scripts/phase-06/13-review-naive-explain.sql',
  'review-aggregate:query-shaped-index:prepare': 'scripts/phase-06/14-review-query-shaped-prepare.sql',
  'review-aggregate:query-shaped-index:explain': 'scripts/phase-06/15-review-query-shaped-explain.sql',
  'monthly-order-aggregate:baseline:prepare': 'scripts/phase-06/20-monthly-baseline-prepare.sql',
  'monthly-order-aggregate:baseline:explain': 'scripts/phase-06/21-monthly-baseline-explain.sql',
  'monthly-order-aggregate:naive-index:prepare': 'scripts/phase-06/22-monthly-naive-prepare.sql',
  'monthly-order-aggregate:naive-index:explain': 'scripts/phase-06/23-monthly-naive-explain.sql',
  'monthly-order-aggregate:query-shaped-index:prepare': 'scripts/phase-06/24-monthly-query-shaped-prepare.sql',
  'monthly-order-aggregate:query-shaped-index:explain': 'scripts/phase-06/25-monthly-query-shaped-explain.sql',
};

function parseArgs(argv) {
  const parsed = {};

  for (let index = 0; index < argv.length; index += 1) {
    const name = argv[index];
    if (!name.startsWith('--')) {
      throw new Error(`Unexpected argument: ${name}`);
    }

    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) {
      throw new Error(`Missing value for ${name}`);
    }

    parsed[name.slice(2)] = value;
    index += 1;
  }

  return parsed;
}

function usage() {
  return [
    'Usage:',
    '  npm run phase:sql -- --phase phase-06 --scenario data-profile --action profile [--output <path>]',
    '  npm run phase:sql -- --phase phase-06 --scenario review-aggregate --condition naive-index --action prepare',
    '  npm run phase:sql -- --phase phase-06 --scenario review-aggregate --condition naive-index --action explain --output <path>',
  ].join('\n');
}

function resolveScript({ phase, scenario, condition, action }) {
  if (phase !== 'phase-06') {
    throw new Error(`Unsupported phase: ${phase}. Only phase-06 is mapped.`);
  }

  if (!scenario) {
    throw new Error('Missing required argument: --scenario');
  }

  if (!action) {
    throw new Error('Missing required argument: --action');
  }

  if (scenario === 'data-profile') {
    if (action !== 'profile') {
      throw new Error('data-profile requires --action profile');
    }
    return PHASE_06_SCRIPTS['data-profile:profile'];
  }

  if (!condition) {
    throw new Error(`${scenario} requires --condition`);
  }

  if (!['prepare', 'explain'].includes(action)) {
    throw new Error(`${scenario} requires --action prepare or --action explain`);
  }

  const key = `${scenario}:${condition}:${action}`;
  const script = PHASE_06_SCRIPTS[key];
  if (!script) {
    throw new Error(`Unsupported Phase 6 SQL mapping: ${key}`);
  }

  return script;
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const phase = args.phase;

  if (!phase) {
    throw new Error('Missing required argument: --phase');
  }

  const script = resolveScript(args);
  const command = [
    'compose',
    'exec',
    '-T',
    'postgres',
    'psql',
    '-U',
    'app',
    '-d',
    'ecommerce',
    '-f',
    '-',
  ];

  const sql = readFileSync(script, 'utf8');
  const result = spawnSync('docker', command, {
    encoding: 'utf8',
    input: sql,
  });

  if (result.error) {
    throw result.error;
  }

  if (result.stderr) {
    process.stderr.write(result.stderr);
  }

  if (args.output) {
    mkdirSync(dirname(args.output), { recursive: true });
    writeFileSync(args.output, result.stdout);
  } else if (result.stdout) {
    process.stdout.write(result.stdout);
  }

  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

try {
  main();
} catch (error) {
  console.error(error.message);
  console.error(usage());
  process.exit(1);
}
