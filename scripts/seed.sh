#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PRESET="${1:-small}"

case "$PRESET" in
  small|loadtest) ;;
  *)
    echo "Usage: $0 [small|loadtest]" >&2
    exit 1
    ;;
esac

cd "$ROOT_DIR/ecommerce"
./gradlew bootRun --args="--spring.profiles.active=seeder,seed-${PRESET}"
