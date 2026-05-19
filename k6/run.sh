#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SCENARIO="${1:-}"
PRESET="${2:-baseline}"
MODE="${3:-local}"
PHASE="${PHASE:-phase-01}"
POOL="${POOL:-pool10}"
K6_TAIL_LINES="${K6_TAIL_LINES:-120}"

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
)

if ! [[ "$K6_TAIL_LINES" =~ ^[0-9]+$ ]] || [[ "$K6_TAIL_LINES" -eq 0 ]]; then
  echo "Invalid K6_TAIL_LINES: $K6_TAIL_LINES" >&2
  exit 1
fi

RESULTS_DIR="${K6_RESULTS_DIR:-$SCRIPT_DIR/results}"
LOG_PRESET="${PRESET//\//_}"
LOG_FILE="${K6_LOG_FILE:-$RESULTS_DIR/${SCENARIO}-${LOG_PRESET}-${MODE}.log}"
RUN_DIR="$SCRIPT_DIR"

case "$MODE" in
  local) ;;
  prometheus)
    RUN_DIR="$ROOT_DIR"
    K6_CMD=(
      env MSYS_NO_PATHCONV=1 docker compose --profile test run --rm k6 \
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

set +e
(
  cd "$RUN_DIR"
  "${K6_CMD[@]}"
) > "$LOG_FILE" 2>&1
STATUS=$?
set -e

echo "k6 log: $LOG_FILE"
echo "Showing last $K6_TAIL_LINES lines:"
tail -n "$K6_TAIL_LINES" "$LOG_FILE"

exit "$STATUS"
