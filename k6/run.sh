#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCENARIO="${1:-}"
PRESET="${2:-baseline}"

if [[ -z "$SCENARIO" ]]; then
  echo "Usage: $0 <orders|products|points> [preset]" >&2
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

if command -v k6 >/dev/null 2>&1; then
  cd "$SCRIPT_DIR"
  exec k6 run -e PRESET="$PRESET_FILE" "$SCRIPT"
fi

exec docker run --rm -i \
  --network host \
  -v "$SCRIPT_DIR:/scripts" \
  -w /scripts \
  grafana/k6 run -e PRESET="$PRESET_FILE" "$SCRIPT"
