#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SCENARIO="${1:-}"
PRESET="${2:-baseline}"
MODE="${3:-local}"
PHASE="${PHASE:-phase-01}"
POOL="${POOL:-pool10}"

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

case "$MODE" in
  local) ;;
  prometheus)
    cd "$ROOT_DIR"
    exec docker compose --profile test run --rm k6 \
      run \
      --out experimental-prometheus-rw \
      "${K6_ARGS[@]}" \
      "/scripts/$SCRIPT"
    ;;
  *)
    echo "Unknown k6 mode: $MODE" >&2
    echo "Usage: $0 <orders|products|points> [preset] [local|prometheus]" >&2
    exit 1
    ;;
esac

if command -v k6 >/dev/null 2>&1; then
  cd "$SCRIPT_DIR"
  exec k6 run "${K6_ARGS[@]}" "$SCRIPT"
fi

exec docker run --rm -i \
  --network host \
  -v "$SCRIPT_DIR:/scripts" \
  -w /scripts \
  grafana/k6 run "${K6_ARGS[@]}" "$SCRIPT"
