import { spawn } from 'node:child_process';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)));

const DEFAULTS = {
  phase: 'phase-01',
  scenario: undefined,
  preset: 'baseline',
  pool: 'pool10',
  mode: 'prometheus',
  condition: undefined,
  capture: false,
  uri: undefined,
  table: undefined,
  output: undefined,
};

function readValue(argv, index, option) {
  const value = argv[index + 1];
  if (value === undefined || value.startsWith('--')) {
    throw new Error(`Missing value for ${option}`);
  }
  return value;
}

function parseArgs(argv) {
  const args = { ...DEFAULTS };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];

    if (arg === '--phase') args.phase = readValue(argv, i, arg);
    else if (arg === '--scenario') args.scenario = readValue(argv, i, arg);
    else if (arg === '--preset') args.preset = readValue(argv, i, arg);
    else if (arg === '--pool') args.pool = readValue(argv, i, arg);
    else if (arg === '--mode') args.mode = readValue(argv, i, arg);
    else if (arg === '--condition') args.condition = readValue(argv, i, arg);
    else if (arg === '--capture') {
      args.capture = true;
      i -= 1;
    } else if (arg === '--uri') args.uri = readValue(argv, i, arg);
    else if (arg === '--table') args.table = readValue(argv, i, arg);
    else if (arg === '--output') args.output = readValue(argv, i, arg);
    else if (arg === '--help') {
      printHelp();
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }

    i += 1;
  }

  if (!args.scenario) {
    throw new Error('Missing required option: --scenario <orders|products|points>');
  }

  return args;
}

function requireValue(value, name) {
  if (value === undefined || value === '') {
    throw new Error(`Missing required option: ${name}`);
  }
}

export function buildEvidencePaths({
  phase,
  scenario,
  preset,
  pool,
  condition,
  output,
}) {
  requireValue(phase, '--phase');
  requireValue(scenario, '--scenario');
  requireValue(preset, '--preset');
  requireValue(pool, '--pool');

  const evidenceCondition = condition ?? `${pool}-${preset}`;
  const evidenceScenario = phase === 'phase-06' && scenario === 'review-summary'
    ? 'review-summary-api'
    : scenario;
  const evidenceDir = `docs/evidence/${phase}/${evidenceScenario}/${evidenceCondition}`;
  const outputName = condition
    ? `${scenario}-${evidenceCondition}.png`
    : `${scenario}-${preset}-${pool}.png`;

  return {
    condition: evidenceCondition,
    evidenceDir,
    measurementFile: `${evidenceDir}/measurement.json`,
    summaryJsonFile: `${evidenceDir}/k6-summary.json`,
    runWindowFile: `${evidenceDir}/run-window.json`,
    exitStatusFile: `${evidenceDir}/k6-exit-status.txt`,
    output: output ?? `docs/evidence/${phase}/grafana-screenshots/${outputName}`,
  };
}

function rangeLabel(start, end) {
  if (start === undefined && end === undefined) {
    return undefined;
  }
  if (start === end || end === undefined) {
    return String(start);
  }
  return `${start}..${end}`;
}

function getStatusLabel(statuses) {
  if (!Array.isArray(statuses) || statuses.length === 0) {
    return 'ON_SALE/SOLD_OUT/DISCONTINUED';
  }
  return statuses.join('/');
}

export function buildTarget({ scenario, preset, presetData, env = process.env }) {
  const userRange = rangeLabel(presetData.userStart ?? 1, presetData.userEnd ?? 1000);

  if (scenario === 'orders') {
    return {
      method: 'GET',
      endpoint: '/api/orders',
      queryParams: {
        userId: userRange,
        strategy: env.STRATEGY || 'lazy',
      },
      strategy: env.STRATEGY || 'lazy',
    };
  }

  if (scenario === 'products') {
    return {
      method: 'GET',
      endpoint: '/api/products',
      queryParams: {
        categoryId: rangeLabel(presetData.categoryStart ?? 1, presetData.categoryEnd ?? 200),
        status: getStatusLabel(presetData.statuses),
        strategy: env.STRATEGY || 'baseline',
      },
      strategy: env.STRATEGY || 'baseline',
    };
  }

  if (scenario === 'points') {
    const queryParams = {
      userId: userRange,
      size: String(presetData.size ?? 20),
    };
    if (presetData.page !== undefined) {
      queryParams.page = String(presetData.page);
    } else {
      queryParams.page = 'weighted';
    }

    return {
      method: 'GET',
      endpoint: '/api/points',
      queryParams,
      strategy: preset,
    };
  }

  return {
    method: 'GET',
    endpoint: `/${scenario}`,
    queryParams: {},
  };
}

function buildWorkload(presetData) {
  return {
    rate: presetData.rate,
    duration: presetData.duration,
    timeout: presetData.timeout,
    preAllocatedVUs: presetData.preAllocatedVUs,
    maxVUs: presetData.maxVUs,
  };
}

function compactObject(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, entry]) => entry !== undefined && entry !== ''),
  );
}

export function buildMeasurement({
  args,
  paths,
  presetData,
  exitStatus,
  env = process.env,
  git = {},
}) {
  const resultStatus = classifyK6ExitStatus(exitStatus);

  return {
    schemaVersion: 1,
    phase: args.phase,
    scenario: args.scenario,
    condition: paths.condition,
    preset: args.preset,
    pool: args.pool,
    mode: args.mode,
    target: buildTarget({
      scenario: args.scenario,
      preset: args.preset,
      presetData,
      env,
    }),
    workload: compactObject(buildWorkload(presetData)),
    evidence: {
      exitStatus,
      files: {
        summaryJson: paths.summaryJsonFile,
        runWindow: paths.runWindowFile,
        exitStatus: paths.exitStatusFile,
      },
    },
    execution: {
      command: buildExecutionCommand(args),
      exitStatus,
      resultStatus,
    },
    git: compactObject({
      commitSha: git.commitSha,
      dirty: git.dirty,
    }),
  };
}

function buildExecutionCommand(args) {
  const parts = ['npm run k6:evidence --'];
  pushOption(parts, '--phase', args.phase);
  pushOption(parts, '--scenario', args.scenario);
  pushOption(parts, '--preset', args.preset);
  pushOption(parts, '--pool', args.pool);
  pushOption(parts, '--mode', args.mode);
  pushOption(parts, '--condition', args.condition);
  if (args.capture) {
    parts.push('--capture');
  }
  pushOption(parts, '--uri', args.uri);
  pushOption(parts, '--table', args.table);
  pushOption(parts, '--output', args.output);
  return parts.join(' ');
}

function readPreset(preset) {
  const path = resolve(ROOT, 'k6', 'presets', `${preset}.json`);
  return JSON.parse(readFileSync(path, 'utf8'));
}

function getGitInfo() {
  async function runGit(args) {
    return new Promise((resolveRun) => {
      const child = spawn('git', args, {
        cwd: ROOT,
        stdio: ['ignore', 'pipe', 'ignore'],
      });
      let output = '';
      child.stdout.on('data', (chunk) => {
        output += chunk;
      });
      child.on('error', () => resolveRun(''));
      child.on('exit', (code) => {
        resolveRun(code === 0 ? output.trim() : '');
      });
    });
  }

  return Promise.all([
    runGit(['rev-parse', 'HEAD']),
    runGit(['status', '--porcelain']),
  ]).then(([commitSha, status]) => ({
    commitSha,
    dirty: status.length > 0,
  }));
}

function writeMeasurement({ args, paths, presetData, exitStatus, git }) {
  const measurement = buildMeasurement({
    args,
    paths,
    presetData,
    exitStatus,
    git,
  });
  mkdirSync(dirname(paths.measurementFile), { recursive: true });
  writeFileSync(paths.measurementFile, `${JSON.stringify(measurement, null, 2)}\n`);
  return measurement;
}

export function classifyK6ExitStatus(exitStatus) {
  const code = Number(exitStatus);
  if (code === 0) {
    return 'passed';
  }
  if (code === 99) {
    return 'threshold_failed';
  }
  return 'execution_failed';
}

export function shouldContinueAfterK6Exit(exitStatus) {
  return classifyK6ExitStatus(exitStatus) !== 'execution_failed';
}

export function buildK6RunEnv({ phase, pool, paths }) {
  return {
    PHASE: phase,
    POOL: pool,
    K6_TAIL_ONLY: '0',
    K6_SUMMARY_JSON_FILE: paths.summaryJsonFile,
    K6_RUN_WINDOW_FILE: paths.runWindowFile,
    K6_EXIT_STATUS_FILE: paths.exitStatusFile,
    K6_WRITE_RUN_WINDOW_ON_FAILURE: '1',
  };
}

function pushOption(args, option, value) {
  if (value !== undefined) {
    args.push(option, value);
  }
}

export function buildGrafanaCaptureArgs({
  phase,
  scenario,
  preset,
  pool,
  uri,
  table,
  runWindowFile,
  output,
}) {
  requireValue(runWindowFile, '--window-file');
  requireValue(output, '--output');

  const args = ['scripts/capture-grafana-dashboard.mjs'];
  pushOption(args, '--phase', phase);
  pushOption(args, '--scenario', scenario);
  pushOption(args, '--preset', preset);
  pushOption(args, '--pool', pool);
  pushOption(args, '--uri', uri);
  pushOption(args, '--table', table);
  pushOption(args, '--window-file', runWindowFile);
  pushOption(args, '--output', output);
  if (phase === 'phase-06') {
    args.push('--no-align-phase-rows');
  }
  return args;
}

function printHelp() {
  console.log(`Usage: npm run k6:evidence -- --phase <phase-id> --scenario <name> [options]

Options:
  --phase <phase-id>      Evidence phase, for example phase-02
  --scenario <name>       k6 scenario: orders, products, or points
  --preset <name>         k6 preset, default baseline
  --pool <name>           pool label, default pool10
  --mode <mode>           k6 mode, default prometheus
  --condition <name>      evidence condition directory, default <pool>-<preset>
  --capture               Capture and stitch Grafana after k6 succeeds
  --uri <pattern>         Grafana URI variable, used with --capture
  --table <pattern>       Grafana table variable, used with --capture
  --output <path>         Final stitched PNG path, used with --capture
`);
}

async function run(command, args, env = {}, options = {}) {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(command, args, {
      cwd: ROOT,
      env: { ...process.env, ...env },
      stdio: 'inherit',
    });

    child.on('error', rejectRun);
    child.on('exit', (code) => {
      const exitCode = code ?? 1;
      if (exitCode === 0 || options.allowAnyExit) {
        resolveRun(exitCode);
      } else {
        rejectRun(new Error(`${command} ${args.join(' ')} exited with code ${exitCode}`));
      }
    });
  });
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const paths = buildEvidencePaths(args);

  console.log(`k6 summary json: ${paths.summaryJsonFile}`);
  console.log(`k6 measurement: ${paths.measurementFile}`);
  console.log(`k6 run window: ${paths.runWindowFile}`);
  console.log(`k6 exit status: ${paths.exitStatusFile}`);
  if (args.capture) {
    console.log(`grafana screenshot: ${paths.output}`);
  }

  const k6ExitStatus = await run(
    'bash',
    ['k6/run.sh', args.scenario, args.preset, args.mode],
    buildK6RunEnv({
      phase: args.phase,
      pool: args.pool,
      paths,
    }),
    { allowAnyExit: true },
  );
  const k6ResultStatus = classifyK6ExitStatus(k6ExitStatus);
  writeMeasurement({
    args,
    paths,
    presetData: readPreset(args.preset),
    exitStatus: k6ExitStatus,
    git: await getGitInfo(),
  });

  console.log(`k6 result: ${k6ResultStatus} (exit ${k6ExitStatus})`);

  if (!shouldContinueAfterK6Exit(k6ExitStatus)) {
    process.exit(k6ExitStatus || 1);
  }

  if (args.capture) {
    await run(process.execPath, buildGrafanaCaptureArgs({
      ...args,
      runWindowFile: paths.runWindowFile,
      output: paths.output,
    }));
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(error.message);
    process.exit(1);
  });
}
