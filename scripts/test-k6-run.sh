#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

mkdir -p "$TMP_DIR/k6/presets" "$TMP_DIR/bin"
cp "$REPO_ROOT/k6/run.sh" "$TMP_DIR/k6/run.sh"
chmod +x "$TMP_DIR/k6/run.sh"

printf 'export default function () {}\n' > "$TMP_DIR/k6/products-test.js"
printf '{}\n' > "$TMP_DIR/k6/presets/baseline.json"

cat > "$TMP_DIR/bin/k6" <<'FAKE_K6'
#!/usr/bin/env bash
for i in $(seq 1 12); do
  printf 'line-%03d\n' "$i"
done
exit 7
FAKE_K6
chmod +x "$TMP_DIR/bin/k6"

set +e
PATH="$TMP_DIR/bin:$PATH" K6_TAIL_LINES=5 "$TMP_DIR/k6/run.sh" products baseline local > "$TMP_DIR/stdout" 2> "$TMP_DIR/stderr"
STATUS=$?
set -e

if [[ "$STATUS" -ne 7 ]]; then
  echo "expected k6 exit status 7, got $STATUS" >&2
  exit 1
fi

if grep -q 'line-001' "$TMP_DIR/stdout"; then
  echo "expected stdout to contain only the tail, but it included early k6 output" >&2
  exit 1
fi

grep -q 'line-008' "$TMP_DIR/stdout"
grep -q 'line-012' "$TMP_DIR/stdout"

LOG_FILE="$TMP_DIR/k6/results/products-baseline-local.log"
if [[ ! -f "$LOG_FILE" ]]; then
  echo "expected log file at $LOG_FILE" >&2
  exit 1
fi

grep -q 'line-001' "$LOG_FILE"
grep -q 'line-012' "$LOG_FILE"

cat > "$TMP_DIR/bin/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
if [[ "${MSYS_NO_PATHCONV:-}" != "1" ]]; then
  echo "missing MSYS_NO_PATHCONV" >&2
  exit 23
fi

last_arg="${@: -1}"
for i in $(seq 1 6); do
  printf 'compose-line-%03d\n' "$i"
done
printf 'last-arg=%s\n' "$last_arg"
exit 9
FAKE_DOCKER
chmod +x "$TMP_DIR/bin/docker"

set +e
PATH="$TMP_DIR/bin:$PATH" K6_TAIL_LINES=3 "$TMP_DIR/k6/run.sh" products baseline prometheus > "$TMP_DIR/prometheus-stdout" 2> "$TMP_DIR/prometheus-stderr"
STATUS=$?
set -e

if [[ "$STATUS" -ne 9 ]]; then
  echo "expected docker compose exit status 9, got $STATUS" >&2
  exit 1
fi

if grep -q 'compose-line-001' "$TMP_DIR/prometheus-stdout"; then
  echo "expected prometheus stdout to contain only the tail, but it included early docker output" >&2
  exit 1
fi

grep -q 'compose-line-005' "$TMP_DIR/prometheus-stdout"
grep -q 'last-arg=/scripts/products-test.js' "$TMP_DIR/prometheus-stdout"

PROMETHEUS_LOG_FILE="$TMP_DIR/k6/results/products-baseline-prometheus.log"
grep -q 'compose-line-001' "$PROMETHEUS_LOG_FILE"
grep -q 'last-arg=/scripts/products-test.js' "$PROMETHEUS_LOG_FILE"
