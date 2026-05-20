import { mkdir, readFile, readdir, rm, stat, writeFile } from 'node:fs/promises';
import { spawn } from 'node:child_process';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  PHASE_FOCUS_ROWS,
  buildCapturePlan,
  buildEvidenceOutputPath,
  buildGrafanaDashboardUrl,
  getGrafanaTimeRangeFromRunWindow,
  getFocusRowTitleForPhase,
  getDashboardVarsFromUrl,
  getPhaseFromUrl,
  shouldRequireRunWindow,
} from './grafana-capture-utils.mjs';

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)));
const DEFAULT_SELECTOR = '[data-testid="data-testid DashboardEditPaneSplitter body container"]';

const DEFAULTS = {
  url: undefined,
  selector: DEFAULT_SELECTOR,
  partsDir: join(ROOT, 'docs/evidence/grafana-internal-scroll-captures'),
  output: undefined,
  viewportWidth: 1600,
  viewportHeight: 965,
  waitMs: 800,
  stitch: true,
  python: process.env.PYTHON || 'python',
  phase: undefined,
  scenario: undefined,
  preset: undefined,
  pool: undefined,
  uri: undefined,
  table: undefined,
  from: undefined,
  to: undefined,
  refresh: undefined,
  windowFile: undefined,
  allowLive: false,
  alignPhaseRows: true,
};

function parseArgs(argv) {
  const args = { ...DEFAULTS };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    const value = argv[i + 1];

    if (arg === '--url') args.url = value;
    else if (arg === '--selector') args.selector = value;
    else if (arg === '--parts-dir') args.partsDir = resolve(ROOT, value);
    else if (arg === '--output') args.output = resolve(ROOT, value);
    else if (arg === '--viewport-width') args.viewportWidth = Number(value);
    else if (arg === '--viewport-height') args.viewportHeight = Number(value);
    else if (arg === '--wait-ms') args.waitMs = Number(value);
    else if (arg === '--python') args.python = value;
    else if (arg === '--phase') args.phase = value;
    else if (arg === '--scenario') args.scenario = value;
    else if (arg === '--preset') args.preset = value;
    else if (arg === '--pool') args.pool = value;
    else if (arg === '--uri') args.uri = value;
    else if (arg === '--table') args.table = value;
    else if (arg === '--from') args.from = value;
    else if (arg === '--to') args.to = value;
    else if (arg === '--refresh') args.refresh = value;
    else if (arg === '--window-file') args.windowFile = resolve(ROOT, value);
    else if (arg === '--live') {
      args.allowLive = true;
      i -= 1;
    }
    else if (arg === '--no-stitch') {
      args.stitch = false;
      i -= 1;
    } else if (arg === '--no-align-phase-rows') {
      args.alignPhaseRows = false;
      i -= 1;
    }
    else if (arg === '--help') {
      printHelp();
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }

    i += 1;
  }

  return args;
}

async function collectRunWindowFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true }).catch((error) => {
    if (error.code === 'ENOENT') {
      return [];
    }
    throw error;
  });

  const files = [];
  for (const entry of entries) {
    const entryPath = join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...await collectRunWindowFiles(entryPath));
    } else if (entry.isFile() && entry.name === 'run-window.json') {
      files.push(entryPath);
    }
  }
  return files;
}

async function findLatestRunWindowFile(vars) {
  const baseDir = join(ROOT, 'docs/evidence', vars.phase, vars.scenario);
  const files = await collectRunWindowFiles(baseDir);
  const matches = [];

  for (const file of files) {
    const metadata = JSON.parse(await readFile(file, 'utf8'));
    if (
      metadata.phase === vars.phase
      && metadata.scenario === vars.scenario
      && metadata.preset === vars.preset
      && metadata.pool === vars.pool
    ) {
      const fileStat = await stat(file);
      matches.push({ file, mtimeMs: fileStat.mtimeMs });
    }
  }

  matches.sort((a, b) => b.mtimeMs - a.mtimeMs);
  return matches[0]?.file;
}

function printHelp() {
  console.log(`Usage: node scripts/capture-grafana-dashboard.mjs [options]

Options:
  --url <url>                 Grafana dashboard URL
  --selector <selector>       Scroll container selector
  --parts-dir <path>          Directory for clipped captures
  --output <path>             Final stitched PNG path
  --viewport-width <number>   Browser viewport width, default 1600
  --viewport-height <number>  Browser viewport height, default 965
  --wait-ms <number>          Wait after render/scroll, default 800
  --python <path>             Python command for stitching, default python
  --phase <phase-id>          Dashboard phase variable and focus row
  --scenario <name>           Dashboard scenario variable
  --preset <name>             Dashboard preset variable
  --pool <name>               Dashboard pool variable
  --uri <pattern>             Dashboard URI variable
  --table <pattern>           Dashboard table variable
  --from <time>               Grafana time range start, for example epoch milliseconds
  --to <time>                 Grafana time range end, for example epoch milliseconds
  --refresh <value>           Grafana refresh parameter, use an empty value to disable refresh
  --window-file <path>        Read grafanaFrom/grafanaTo from a k6 run-window JSON file. If omitted, the latest matching file under docs/evidence/<phase>/<scenario>/ is used when present.
  --live                      Allow live now-30m capture when no run-window file is found
  --no-stitch                 Save parts only, skip final PNG stitching
  --no-align-phase-rows       Keep existing phase focus row states
`);
}

async function resolveCaptureConfig(args) {
  const url = args.url ?? buildGrafanaDashboardUrl(args);
  const urlVars = getDashboardVarsFromUrl(url);
  const vars = {
    phase: args.phase ?? urlVars.phase,
    scenario: args.scenario ?? urlVars.scenario,
    preset: args.preset ?? urlVars.preset,
    pool: args.pool ?? urlVars.pool,
    uri: args.uri ?? urlVars.uri,
    table: args.table ?? urlVars.table,
  };
  const windowFile = args.windowFile ?? await findLatestRunWindowFile(vars);
  const hasExplicitTimeRange = args.from !== undefined || args.to !== undefined;
  const hasExplicitDashboardVars = Boolean(args.phase || args.scenario || args.preset || args.pool);
  if (!windowFile && shouldRequireRunWindow({
    hasExplicitDashboardVars,
    hasExplicitTimeRange,
    hasUrl: Boolean(args.url),
    allowLive: args.allowLive,
  })) {
    throw new Error(`No matching run-window.json found for phase=${vars.phase}, scenario=${vars.scenario}, preset=${vars.preset}, pool=${vars.pool}. Run k6 first or pass --live for an explicit live capture.`);
  }

  const windowTimeRange = windowFile
    ? getGrafanaTimeRangeFromRunWindow(JSON.parse(await readFile(windowFile, 'utf8')))
    : {};
  const timeRange = { ...windowTimeRange };
  if (args.from !== undefined) timeRange.from = args.from;
  if (args.to !== undefined) timeRange.to = args.to;
  if (args.refresh !== undefined) timeRange.refresh = args.refresh;
  const finalUrl = args.url
    ? applyDashboardConfigToUrl(url, vars, timeRange)
    : buildGrafanaDashboardUrl({ ...vars, ...timeRange });
  const output = args.output ?? resolve(ROOT, buildEvidenceOutputPath(vars));

  return { url: finalUrl, output, vars, windowFile };
}

function applyDashboardConfigToUrl(url, vars, timeRange) {
  const nextUrl = new URL(url);
  nextUrl.searchParams.set('var-phase', vars.phase);
  nextUrl.searchParams.set('var-scenario', vars.scenario);
  nextUrl.searchParams.set('var-preset', vars.preset);
  nextUrl.searchParams.set('var-pool', vars.pool);
  nextUrl.searchParams.set('var-uri', vars.uri);
  nextUrl.searchParams.set('var-table', vars.table);
  if (timeRange.from !== undefined) nextUrl.searchParams.set('from', timeRange.from);
  if (timeRange.to !== undefined) nextUrl.searchParams.set('to', timeRange.to);
  if (timeRange.refresh !== undefined) nextUrl.searchParams.set('refresh', timeRange.refresh);
  return nextUrl.toString();
}

async function loadChromium() {
  try {
    const mod = await import('playwright');
    return mod.chromium;
  } catch (error) {
    if (error.code !== 'ERR_MODULE_NOT_FOUND') {
      throw error;
    }
  }

  throw new Error('Playwright is not installed. Run: npm install');
}

async function launchBrowser(chromium) {
  const executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE;
  if (executablePath) {
    return chromium.launch({ executablePath, headless: true });
  }

  for (const channel of [process.env.PLAYWRIGHT_CHANNEL, 'msedge', 'chrome'].filter(Boolean)) {
    try {
      return await chromium.launch({ channel, headless: true });
    } catch {
      // Try the next locally installed browser channel.
    }
  }

  return chromium.launch({ headless: true });
}

async function getContainerInfo(page, selector) {
  return page.locator(selector).evaluate((el) => {
    const rect = el.getBoundingClientRect();
    return {
      scrollTop: Math.round(el.scrollTop),
      scrollHeight: Math.round(el.scrollHeight),
      clientHeight: Math.round(el.clientHeight),
      clientWidth: Math.round(el.clientWidth),
      rect: {
        x: Math.round(rect.x),
        y: Math.round(rect.y),
        width: Math.round(rect.width),
        height: Math.round(rect.height),
      },
    };
  });
}

async function wait(ms) {
  await new Promise((resolveWait) => setTimeout(resolveWait, ms));
}

async function runStitch({ python, partsDir, output }) {
  const metadata = join(partsDir, 'capture-meta.json');
  const args = [
    'scripts/stitch_grafana_dashboard.py',
    '--input-dir',
    partsDir,
    '--output',
    output,
    '--metadata',
    metadata,
  ];

  await new Promise((resolveRun, rejectRun) => {
    const child = spawn(python, args, {
      cwd: ROOT,
      stdio: 'inherit',
    });

    child.on('error', rejectRun);
    child.on('exit', (code) => {
      if (code === 0) {
        resolveRun();
      } else {
        rejectRun(new Error(`stitch command exited with code ${code}`));
      }
    });
  });
}

async function capturePart({ page, selector, offset, index, partsDir, waitMs }) {
  const info = await page.locator(selector).evaluate((el, requestedOffset) => {
    el.scrollTop = requestedOffset;
    el.dispatchEvent(new Event('scroll', { bubbles: true }));
    const rect = el.getBoundingClientRect();

    return {
      requestedOffset,
      actualOffset: Math.round(el.scrollTop),
      scrollHeight: Math.round(el.scrollHeight),
      clientHeight: Math.round(el.clientHeight),
      clientWidth: Math.round(el.clientWidth),
      rect: {
        x: Math.round(rect.x),
        y: Math.round(rect.y),
        width: Math.round(rect.width),
        height: Math.round(rect.height),
      },
    };
  }, offset);

  await wait(waitMs);

  const fileName = `part-${String(index + 1).padStart(2, '0')}-scroll${info.actualOffset}.png`;
  const filePath = join(partsDir, fileName);
  await page.screenshot({ path: filePath, clip: info.rect });

  return { ...info, file: fileName };
}

async function clickIfPresent(locator) {
  const count = await locator.count();
  if (count === 0) {
    return false;
  }
  if (count !== 1) {
    throw new Error(`Expected one row toggle, found ${count}`);
  }

  await locator.click();
  return true;
}

async function setScrollTop(page, selector, offset) {
  return page.locator(selector).evaluate((el, requestedOffset) => {
    el.scrollTop = requestedOffset;
    el.dispatchEvent(new Event('scroll', { bubbles: true }));
    return Math.round(el.scrollTop);
  }, offset);
}

async function alignPhaseFocusRows(page, phase) {
  const targetTitle = getFocusRowTitleForPhase(phase);

  for (const title of Object.values(PHASE_FOCUS_ROWS)) {
    await clickIfPresent(page.getByRole('button', { name: `Collapse row ${title}` }));
  }

  await clickIfPresent(page.getByRole('button', { name: `Expand row ${targetTitle}` }));
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const config = await resolveCaptureConfig(args);
  const chromium = await loadChromium();
  const browser = await launchBrowser(chromium);

  try {
    const context = await browser.newContext({
      viewport: { width: args.viewportWidth, height: args.viewportHeight },
    });
    const page = await context.newPage();

    await page.goto(config.url, { waitUntil: 'domcontentloaded' });
    await page.locator(args.selector).waitFor({ state: 'visible', timeout: 30_000 });
    await wait(args.waitMs);
    const phase = config.vars.phase ?? getPhaseFromUrl(config.url);

    if (args.alignPhaseRows) {
      await alignPhaseFocusRows(page, phase);
      await setScrollTop(page, args.selector, 0);
      await wait(args.waitMs);
    }

    const initialInfo = await getContainerInfo(page, args.selector);
    const offsets = buildCapturePlan({
      scrollHeight: initialInfo.scrollHeight,
      clientHeight: initialInfo.clientHeight,
    });

    await rm(args.partsDir, { recursive: true, force: true });
    await mkdir(args.partsDir, { recursive: true });

    const captures = [];
    for (let index = 0; index < offsets.length; index += 1) {
      captures.push(
        await capturePart({
          page,
          selector: args.selector,
          offset: offsets[index],
          index,
          partsDir: args.partsDir,
          waitMs: args.waitMs,
        }),
      );
    }

    const metadata = {
      url: config.url,
      selector: args.selector,
      viewport: { width: args.viewportWidth, height: args.viewportHeight },
      variables: config.vars,
      windowFile: config.windowFile,
      alignPhaseRows: args.alignPhaseRows,
      output: config.output,
      initial: initialInfo,
      offsets,
      captures,
    };

    await writeFile(join(args.partsDir, 'capture-meta.json'), `${JSON.stringify(metadata, null, 2)}\n`);

    console.log(`captures=${captures.length}`);
    console.log(`partsDir=${args.partsDir}`);
    console.log(`meta=${join(args.partsDir, 'capture-meta.json')}`);
    if (args.stitch) {
      await runStitch({ python: args.python, partsDir: args.partsDir, output: config.output });
      console.log(`output=${config.output}`);
    } else {
      console.log(`stitch with: ${args.python} scripts/stitch_grafana_dashboard.py --input-dir ${args.partsDir} --output ${config.output} --metadata ${join(args.partsDir, 'capture-meta.json')}`);
    }
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
