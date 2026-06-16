import { mkdir, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';

function requiredEnv(name) {
  const value = process.env[name];
  if (value === undefined || value === '') {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function numberEnv(name) {
  const value = Number(requiredEnv(name));
  if (!Number.isFinite(value)) {
    throw new Error(`Invalid numeric environment variable: ${name}`);
  }
  return value;
}

async function main() {
  const output = requiredEnv('K6_RUN_WINDOW_FILE');
  const startedAt = numberEnv('STARTED_AT');
  const endedAt = numberEnv('ENDED_AT');
  const startPaddingMs = numberEnv('START_PADDING_MS');
  const endPaddingMs = numberEnv('END_PADDING_MS');

  const metadata = {
    phase: process.env.PHASE,
    scenario: process.env.SCENARIO,
    preset: process.env.PRESET,
    pool: process.env.POOL,
    mode: process.env.MODE,
    exitStatus: numberEnv('STATUS'),
    resultStatus: process.env.RESULT_STATUS,
    startedAt,
    endedAt,
    grafanaFrom: startedAt - startPaddingMs,
    grafanaTo: endedAt + endPaddingMs,
  };

  await mkdir(dirname(output), { recursive: true });
  await writeFile(output, `${JSON.stringify(metadata, null, 2)}\n`);
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
