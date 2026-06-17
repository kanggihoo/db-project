#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SCENARIO="${1:-}"
PRESET="${2:-baseline}"
MODE="${3:-local}"
PHASE="${PHASE:-phase-01}"
POOL="${POOL:-pool10}"
STRATEGY="${STRATEGY:-}"
K6_TAIL_ONLY="${K6_TAIL_ONLY:-0}"
K6_TAIL_LINES="${K6_TAIL_LINES:-120}"
K6_RUN_WINDOW_FILE="${K6_RUN_WINDOW_FILE:-auto}"
K6_EXIT_STATUS_FILE="${K6_EXIT_STATUS_FILE:-auto}"
K6_SUMMARY_JSON_FILE="${K6_SUMMARY_JSON_FILE:-auto}"
K6_SUMMARY_TREND_STATS="${K6_SUMMARY_TREND_STATS:-avg,min,med,max,p(90),p(95),p(99)}"
K6_SUMMARY_TIME_UNIT="${K6_SUMMARY_TIME_UNIT:-ms}"
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

if [[ -z "$STRATEGY" ]]; then
  case "$SCENARIO" in
    orders) STRATEGY="lazy" ;;
    products) STRATEGY="baseline" ;;
  esac
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

<<<<<<< HEAD
=======
if [[ "$K6_WRITE_RUN_WINDOW_ON_FAILURE" != "0" && "$K6_WRITE_RUN_WINDOW_ON_FAILURE" != "1" ]]; then
  echo "Invalid K6_WRITE_RUN_WINDOW_ON_FAILURE: $K6_WRITE_RUN_WINDOW_ON_FAILURE" >&2
  exit 1
fi

if [[ -z "$K6_SUMMARY_TREND_STATS" ]]; then
  echo "Invalid K6_SUMMARY_TREND_STATS: empty" >&2
  exit 1
fi

if [[ -z "$K6_SUMMARY_TIME_UNIT" ]]; then
  echo "Invalid K6_SUMMARY_TIME_UNIT: empty" >&2
  exit 1
fi

>>>>>>> main
RESULTS_DIR="${K6_RESULTS_DIR:-$SCRIPT_DIR/results}"
LOG_PRESET="${PRESET//\//_}"
LOG_FILE="${K6_LOG_FILE:-}"
RUN_DIR="$SCRIPT_DIR"
SUMMARY_TEMP_HOST_FILE=""
ARTIFACT_DIR="$RESULTS_DIR"
RESULT_STATUS="execution_failed"

if [[ "$LOG_FILE" != "" && "$LOG_FILE" != "0" ]]; then
  ARTIFACT_DIR="$(dirname "$LOG_FILE")"
fi

if [[ "$K6_RUN_WINDOW_FILE" == "auto" ]]; then
  K6_RUN_WINDOW_FILE="$ARTIFACT_DIR/run-window.json"
fi

if [[ "$K6_EXIT_STATUS_FILE" == "auto" ]]; then
  K6_EXIT_STATUS_FILE="$ARTIFACT_DIR/k6-exit-status.txt"
fi

if [[ "$K6_SUMMARY_JSON_FILE" == "auto" ]]; then
  K6_SUMMARY_JSON_FILE="$ARTIFACT_DIR/k6-summary.json"
fi

K6_RUN_ARGS=(
  --summary-mode=full
  --summary-trend-stats "$K6_SUMMARY_TREND_STATS"
  --summary-time-unit "$K6_SUMMARY_TIME_UNIT"
)

if [[ "$K6_SUMMARY_JSON_FILE" != "0" ]]; then
  SUMMARY_TEMP_HOST_FILE="$SCRIPT_DIR/results/${SCENARIO}-${LOG_PRESET}-${MODE}-summary-$$.json"
  mkdir -p "$(dirname "$K6_SUMMARY_JSON_FILE")" "$(dirname "$SUMMARY_TEMP_HOST_FILE")"
  K6_RUN_ARGS+=(--summary-export "/scripts/results/$(basename "$SUMMARY_TEMP_HOST_FILE")")
fi

if [[ "$K6_RUN_WINDOW_FILE" == "auto" ]]; then
  K6_RUN_WINDOW_FILE="$(dirname "$LOG_FILE")/run-window.json"
fi

case "$MODE" in
  local)
    K6_CMD=(
      docker run --rm -i
      --network host
      -v "$SCRIPT_DIR:/scripts"
      -w /scripts
      grafana/k6 run
      "${K6_RUN_ARGS[@]}"
      "${K6_ARGS[@]}"
      "$SCRIPT"
    )
    ;;
  prometheus)
    RUN_DIR="$ROOT_DIR"
    # Prevent Git Bash from rewriting Docker container paths like /scripts/*.js.
    export MSYS_NO_PATHCONV="${MSYS_NO_PATHCONV:-1}"
    K6_CMD=(
      docker compose --profile test run --rm k6 \
      run \
      "${K6_RUN_ARGS[@]}" \
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

PROGRESS_PID=""
TTY_OUT=""
if [[ ! -t 1 ]] && (: > /dev/tty) 2>/dev/null; then
  TTY_OUT="/dev/tty"
fi
trap '[[ "$PROGRESS_PID" != "" ]] && kill "$PROGRESS_PID" >/dev/null 2>&1 || true' EXIT INT TERM

print_line() {
  if [[ "$TTY_OUT" != "" ]]; then
    printf '%s\n' "$*" > /dev/tty
  else
    printf '%s\n' "$*"
  fi
}

if [[ "$K6_TAIL_ONLY" == "1" && ( "$LOG_FILE" == "" || "$LOG_FILE" == "0" ) ]]; then
  echo "K6_TAIL_ONLY requires K6_LOG_FILE" >&2
  exit 1
fi

if [[ "$LOG_FILE" != "" && "$LOG_FILE" != "0" ]]; then
  mkdir -p "$(dirname "$LOG_FILE")"
  print_line "k6 output log: $LOG_FILE"
fi

print_line "k6 run started: phase=$PHASE scenario=$SCENARIO preset=$PRESET pool=$POOL mode=$MODE strategy=$STRATEGY"
print_line "k6 command: ${K6_CMD[*]}"

set +e
STARTED_AT="$(node -e "console.log(Date.now())")"
<<<<<<< HEAD
=======
(
  elapsed=0
  while true; do
    sleep 1 || exit 0
    elapsed=$((elapsed + 1))
    print_line "k6 progress: elapsed=${elapsed}s phase=$PHASE scenario=$SCENARIO preset=$PRESET pool=$POOL mode=$MODE strategy=$STRATEGY"
  done
) &
PROGRESS_PID="$!"
>>>>>>> main
if [[ "$K6_TAIL_ONLY" == "1" ]]; then
  (
    cd "$RUN_DIR"
    "${K6_CMD[@]}"
  ) > "$LOG_FILE" 2>&1
  STATUS=$?
elif [[ "$LOG_FILE" != "" && "$LOG_FILE" != "0" ]]; then
  if [[ "$TTY_OUT" == "" ]]; then
    (
      cd "$RUN_DIR"
      "${K6_CMD[@]}"
    ) 2>&1 | tee "$LOG_FILE"
  else
    (
      cd "$RUN_DIR"
      "${K6_CMD[@]}"
    ) 2>&1 | tee "$LOG_FILE" > /dev/tty
  fi
  STATUS=${PIPESTATUS[0]}
else
  if [[ "$TTY_OUT" == "" ]]; then
    (
      cd "$RUN_DIR"
      "${K6_CMD[@]}"
    )
    STATUS=$?
  else
    (
      cd "$RUN_DIR"
      "${K6_CMD[@]}"
    ) > /dev/tty 2>&1
    STATUS=$?
  fi
fi
<<<<<<< HEAD
ENDED_AT="$(node -e "console.log(Date.now())")"
set -e

if [[ "$STATUS" -eq 0 && "$K6_RUN_WINDOW_FILE" != "0" ]]; then
=======
kill "$PROGRESS_PID" >/dev/null 2>&1 || true
wait "$PROGRESS_PID" 2>/dev/null || true
PROGRESS_PID=""
ENDED_AT="$(node -e "console.log(Date.now())")"
set -e

case "$STATUS" in
  0) RESULT_STATUS="passed" ;;
  99) RESULT_STATUS="threshold_failed" ;;
  *) RESULT_STATUS="execution_failed" ;;
esac

if [[ "$K6_SUMMARY_JSON_FILE" != "0" ]]; then
  if [[ -f "$SUMMARY_TEMP_HOST_FILE" ]]; then
    cp "$SUMMARY_TEMP_HOST_FILE" "$K6_SUMMARY_JSON_FILE"
    rm -f "$SUMMARY_TEMP_HOST_FILE"
    print_line "k6 summary json: $K6_SUMMARY_JSON_FILE"
  elif [[ "$STATUS" -eq 0 || "$STATUS" -eq 99 ]]; then
    echo "Missing k6 summary json: $SUMMARY_TEMP_HOST_FILE" >&2
  fi
fi

if [[ "$K6_EXIT_STATUS_FILE" != "0" ]]; then
  mkdir -p "$(dirname "$K6_EXIT_STATUS_FILE")"
  printf "%s\n" "$STATUS" > "$K6_EXIT_STATUS_FILE"
  print_line "k6 exit status: $K6_EXIT_STATUS_FILE"
fi

if [[ "$K6_RUN_WINDOW_FILE" != "0" && ( "$STATUS" -eq 0 || "$K6_WRITE_RUN_WINDOW_ON_FAILURE" == "1" ) ]]; then
>>>>>>> main
  K6_RUN_WINDOW_FILE="$K6_RUN_WINDOW_FILE" \
  PHASE="$PHASE" \
  SCENARIO="$SCENARIO" \
  PRESET="$PRESET" \
  POOL="$POOL" \
  MODE="$MODE" \
<<<<<<< HEAD
=======
  STATUS="$STATUS" \
  RESULT_STATUS="$RESULT_STATUS" \
>>>>>>> main
  STARTED_AT="$STARTED_AT" \
  ENDED_AT="$ENDED_AT" \
  START_PADDING_MS="$K6_WINDOW_START_PADDING_MS" \
  END_PADDING_MS="$K6_WINDOW_END_PADDING_MS" \
<<<<<<< HEAD
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
      startedAt,
      endedAt,
      grafanaFrom: startedAt - startPaddingMs,
      grafanaTo: endedAt + endPaddingMs
    };

    await mkdir(dirname(output), { recursive: true });
    await writeFile(output, `${JSON.stringify(metadata, null, 2)}\n`);
  '
  echo "k6 run window: $K6_RUN_WINDOW_FILE"
=======
  node "$ROOT_DIR/scripts/write-k6-run-window.mjs"
  print_line "k6 run window: $K6_RUN_WINDOW_FILE"
>>>>>>> main
fi

if [[ "$K6_TAIL_ONLY" == "1" ]]; then
  print_line "Showing last $K6_TAIL_LINES lines:"
  tail -n "$K6_TAIL_LINES" "$LOG_FILE" | while IFS= read -r line; do
    print_line "$line"
  done
fi

exit "$STATUS"
