import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { spawnSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

export function parseArgs(argv) {
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

export function resolveSqlFile({ file }) {
  if (!file) {
    throw new Error('Missing required argument: --file');
  }

  return file;
}

export function buildPsqlCommand() {
  return [
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
  ];
}

export function runPhaseSql(args, dependencies = {}) {
  const {
    readFileSync: readFile = readFileSync,
    mkdirSync: mkdir = mkdirSync,
    writeFileSync: writeFile = writeFileSync,
    spawnSync: spawn = spawnSync,
    stdout = process.stdout,
    stderr = process.stderr,
  } = dependencies;

  const sql = readFile(resolveSqlFile(args), 'utf8');
  const result = spawn('docker', buildPsqlCommand(), {
    encoding: 'utf8',
    input: sql,
  });

  if (result.error) {
    throw result.error;
  }

  if (result.stderr) {
    stderr.write(result.stderr);
  }

  if (args.output) {
    mkdir(dirname(args.output), { recursive: true });
    writeFile(args.output, result.stdout);
  } else if (result.stdout) {
    stdout.write(result.stdout);
  }

  return result.status ?? 1;
}

function usage() {
  return [
    'Usage:',
    '  npm run phase:sql -- --file scripts/phase-01/10-products-baseline-explain.sql [--output <path>]',
    '  make phase-sql FILE=scripts/phase-01/10-products-baseline-explain.sql OUTPUT=docs/evidence/phase-01/.../explain.txt',
  ].join('\n');
}

function main() {
  const status = runPhaseSql(parseArgs(process.argv.slice(2)));
  if (status !== 0) {
    process.exit(status);
  }
}

const isCli = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;

if (isCli) {
  try {
    main();
  } catch (error) {
    console.error(error.message);
    console.error(usage());
    process.exit(1);
  }
}
