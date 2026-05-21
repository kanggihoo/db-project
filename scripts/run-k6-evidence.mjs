import { spawn } from 'node:child_process';
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
  const evidenceDir = `docs/evidence/${phase}/${scenario}/${evidenceCondition}`;
  const outputName = condition
    ? `${scenario}-${evidenceCondition}.png`
    : `${scenario}-${preset}-${pool}.png`;

  return {
    condition: evidenceCondition,
    evidenceDir,
    logFile: `${evidenceDir}/k6-summary.txt`,
    runWindowFile: `${evidenceDir}/run-window.json`,
    output: output ?? `docs/evidence/${phase}/grafana-screenshots/${outputName}`,
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

async function run(command, args, env = {}) {
  await new Promise((resolveRun, rejectRun) => {
    const child = spawn(command, args, {
      cwd: ROOT,
      env: { ...process.env, ...env },
      stdio: 'inherit',
    });

    child.on('error', rejectRun);
    child.on('exit', (code) => {
      if (code === 0) {
        resolveRun();
      } else {
        rejectRun(new Error(`${command} ${args.join(' ')} exited with code ${code}`));
      }
    });
  });
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const paths = buildEvidencePaths(args);

  console.log(`k6 evidence log: ${paths.logFile}`);
  console.log(`k6 run window: ${paths.runWindowFile}`);
  if (args.capture) {
    console.log(`grafana screenshot: ${paths.output}`);
  }

  await run('bash', ['k6/run.sh', args.scenario, args.preset, args.mode], {
    PHASE: args.phase,
    POOL: args.pool,
    K6_LOG_FILE: paths.logFile,
    K6_RUN_WINDOW_FILE: paths.runWindowFile,
  });

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
