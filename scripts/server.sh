#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
POOL="${1:-pool10}"

case "$POOL" in
  pool5|pool10|pool20|phase3-batch) ;;
  *)
    echo "Usage: $0 [pool5|pool10|pool20|phase3-batch]" >&2
    exit 1
    ;;
esac

cd "$ROOT_DIR/ecommerce"
./gradlew bootRun --args="--spring.profiles.active=${POOL}"
