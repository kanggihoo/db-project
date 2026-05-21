#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SCENARIO="${1:-}"
PRESET="${2:-baseline}"
MODE="${3:-local}"
PHASE="${PHASE:-phase-01}"
POOL="${POOL:-pool10}"
STRATEGY="${STRATEGY:-lazy}"
K6_TAIL_ONLY="${K6_TAIL_ONLY:-0}"
K6_TAIL_LINES="${K6_TAIL_LINES:-120}"
K6_RUN_WINDOW_FILE="${K6_RUN_WINDOW_FILE:-auto}"
K6_WRITE_RUN_WINDOW_ON_FAILURE="${K6_WRITE_RUN_WINDOW_ON_FAILURE:-0}"
K6_WINDOW_START_PADDING_MS="${K6_WINDOW_START_PADDING_MS:-10000}"
K6_WINDOW_END_PADDING_MS="${K6_WINDOW_END_PADDING_MS:-20000}"

if [[ -z "$SCENARIO" ]]; then
  echo "Usage: $0 <orders|products|points> [preset] [local|prometheus]" >&2
  exit 1
fi

SCRIPT="${SCENARIO}-test.js"
PRESET_FILE="presets/${PRESET}.json"

if [[ ! -f "$SCRIPT_DIR/$SCRIPT" ]]; then
  echo "Unknown k6 scenario: $SCENARIO" >&2
  exit 1
fi

if [[ ! -f "$SCRIPT_DIR/$PRESET_FILE" ]]; then
  echo "Unknown k6 preset: $PRESET" >&2
  exit 1
fi

K6_ARGS=(
  -e PRESET="$PRESET_FILE"
  -e PHASE="$PHASE"
  -e SCENARIO="$SCENARIO"
  -e PRESET_NAME="$PRESET"
  -e POOL="$POOL"
  -e STRATEGY="$STRATEGY"
)

if ! [[ "$K6_TAIL_LINES" =~ ^[0-9]+$ ]] || [[ "$K6_TAIL_LINES" -eq 0 ]]; then
  echo "Invalid K6_TAIL_LINES: $K6_TAIL_LINES" >&2
  exit 1
fi

if [[ "$K6_TAIL_ONLY" != "0" && "$K6_TAIL_ONLY" != "1" ]]; then
  echo "Invalid K6_TAIL_ONLY: $K6_TAIL_ONLY" >&2
  exit 1
fi

if ! [[ "$K6_WINDOW_START_PADDING_MS" =~ ^[0-9]+$ ]]; then
  echo "Invalid K6_WINDOW_START_PADDING_MS: $K6_WINDOW_START_PADDING_MS" >&2
  exit 1
fi

if ! [[ "$K6_WINDOW_END_PADDING_MS" =~ ^[0-9]+$ ]]; then
  echo "Invalid K6_WINDOW_END_PADDING_MS: $K6_WINDOW_END_PADDING_MS" >&2
  exit 1
fi

if [[ "$K6_WRITE_RUN_WINDOW_ON_FAILURE" != "0" && "$K6_WRITE_RUN_WINDOW_ON_FAILURE" != "1" ]]; then
  echo "Invalid K6_WRITE_RUN_WINDOW_ON_FAILURE: $K6_WRITE_RUN_WINDOW_ON_FAILURE" >&2
  exit 1
fi

RESULTS_DIR="${K6_RESULTS_DIR:-$SCRIPT_DIR/results}"
LOG_PRESET="${PRESET//\//_}"
LOG_FILE="${K6_LOG_FILE:-$RESULTS_DIR/${SCENARIO}-${LOG_PRESET}-${MODE}.log}"
RUN_DIR="$SCRIPT_DIR"

if [[ "$K6_RUN_WINDOW_FILE" == "auto" ]]; then
  K6_RUN_WINDOW_FILE="$(dirname "$LOG_FILE")/run-window.json"
fi

case "$MODE" in
  local) ;;
  prometheus)
    RUN_DIR="$ROOT_DIR"
    # Prevent Git Bash from rewriting Docker container paths like /scripts/*.js.
    export MSYS_NO_PATHCONV="${MSYS_NO_PATHCONV:-1}"
    K6_CMD=(
      docker compose --profile test run --rm k6 \
      run \
      --out experimental-prometheus-rw \
      "${K6_ARGS[@]}" \
      "/scripts/$SCRIPT"
    )
    ;;
  *)
    echo "Unknown k6 mode: $MODE" >&2
    echo "Usage: $0 <orders|products|points> [preset] [local|prometheus]" >&2
    exit 1
    ;;
esac

if [[ "$MODE" == "local" ]]; then
  if command -v k6 >/dev/null 2>&1; then
    K6_CMD=(k6 run "${K6_ARGS[@]}" "$SCRIPT")
  else
    K6_CMD=(
      docker run --rm -i
      --network host
      -v "$SCRIPT_DIR:/scripts"
      -w /scripts
      grafana/k6 run "${K6_ARGS[@]}" "$SCRIPT"
    )
  fi
fi

mkdir -p "$(dirname "$LOG_FILE")"

echo "k6 log: $LOG_FILE"

set +e
STARTED_AT="$(node -e "console.log(Date.now())")"
if [[ "$K6_TAIL_ONLY" == "1" ]]; then
  (
    cd "$RUN_DIR"
    "${K6_CMD[@]}"
  ) > "$LOG_FILE" 2>&1
  STATUS=$?
else
  (
    cd "$RUN_DIR"
    "${K6_CMD[@]}"
  ) 2>&1 | tee "$LOG_FILE"
  STATUS=${PIPESTATUS[0]}
fi
ENDED_AT="$(node -e "console.log(Date.now())")"
set -e

if [[ "$K6_RUN_WINDOW_FILE" != "0" && ( "$STATUS" -eq 0 || "$K6_WRITE_RUN_WINDOW_ON_FAILURE" == "1" ) ]]; then
  K6_RUN_WINDOW_FILE="$K6_RUN_WINDOW_FILE" \
  PHASE="$PHASE" \
  SCENARIO="$SCENARIO" \
  PRESET="$PRESET" \
  POOL="$POOL" \
  MODE="$MODE" \
  STATUS="$STATUS" \
  STARTED_AT="$STARTED_AT" \
  ENDED_AT="$ENDED_AT" \
  START_PADDING_MS="$K6_WINDOW_START_PADDING_MS" \
  END_PADDING_MS="$K6_WINDOW_END_PADDING_MS" \
  node --input-type=module -e '
    import { mkdir, writeFile } from "node:fs/promises";
    import { dirname } from "node:path";

    const startedAt = Number(process.env.STARTED_AT);
    const endedAt = Number(process.env.ENDED_AT);
    const startPaddingMs = Number(process.env.START_PADDING_MS);
    const endPaddingMs = Number(process.env.END_PADDING_MS);
    const output = process.env.K6_RUN_WINDOW_FILE;
    const metadata = {
      phase: process.env.PHASE,
      scenario: process.env.SCENARIO,
      preset: process.env.PRESET,
      pool: process.env.POOL,
      mode: process.env.MODE,
      exitStatus: Number(process.env.STATUS),
      startedAt,
      endedAt,
      grafanaFrom: startedAt - startPaddingMs,
      grafanaTo: endedAt + endPaddingMs
    };

    await mkdir(dirname(output), { recursive: true });
    await writeFile(output, `${JSON.stringify(metadata, null, 2)}\n`);
  '
  echo "k6 run window: $K6_RUN_WINDOW_FILE"
fi

if [[ "$K6_TAIL_ONLY" == "1" ]]; then
  echo "Showing last $K6_TAIL_LINES lines:"
  tail -n "$K6_TAIL_LINES" "$LOG_FILE"
fi

exit "$STATUS"
