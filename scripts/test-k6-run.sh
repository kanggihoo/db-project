#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

mkdir -p "$TMP_DIR/k6/presets" "$TMP_DIR/bin" "$TMP_DIR/scripts"
cp "$REPO_ROOT/k6/run.sh" "$TMP_DIR/k6/run.sh"
cp "$REPO_ROOT/scripts/write-k6-run-window.mjs" "$TMP_DIR/scripts/write-k6-run-window.mjs"
chmod +x "$TMP_DIR/k6/run.sh"

printf 'export default function () {}\n' > "$TMP_DIR/k6/products-test.js"
printf '{}\n' > "$TMP_DIR/k6/presets/baseline.json"

RUN_WINDOW_FILE="$TMP_DIR/run-window/run-window.json"
K6_RUN_WINDOW_FILE="$RUN_WINDOW_FILE" \
PHASE="phase-test" \
SCENARIO="products" \
PRESET="baseline" \
POOL="pool10" \
MODE="local" \
STATUS="99" \
RESULT_STATUS="threshold_failed" \
STARTED_AT="1000" \
ENDED_AT="2000" \
START_PADDING_MS="100" \
END_PADDING_MS="250" \
node "$REPO_ROOT/scripts/write-k6-run-window.mjs"

grep -q '"phase": "phase-test"' "$RUN_WINDOW_FILE"
grep -q '"exitStatus": 99' "$RUN_WINDOW_FILE"
grep -q '"resultStatus": "threshold_failed"' "$RUN_WINDOW_FILE"
grep -q '"grafanaFrom": 900' "$RUN_WINDOW_FILE"
grep -q '"grafanaTo": 2250' "$RUN_WINDOW_FILE"

cat > "$TMP_DIR/bin/docker" <<'FAKE_DOCKER_RUN'
#!/usr/bin/env bash
printf 'args=%s\n' "$*"
for i in $(seq 1 12); do
  printf 'line-%03d\n' "$i"
done
exit 7
FAKE_DOCKER_RUN
chmod +x "$TMP_DIR/bin/docker"

set +e
PATH="$TMP_DIR/bin:$PATH" K6_TAIL_LINES=5 "$TMP_DIR/k6/run.sh" products baseline local > "$TMP_DIR/stdout" 2> "$TMP_DIR/stderr"
STATUS=$?
set -e

if [[ "$STATUS" -ne 7 ]]; then
  echo "expected docker run exit status 7, got $STATUS" >&2
  exit 1
fi

grep -q 'k6 run started: phase=phase-01 scenario=products preset=baseline pool=pool10 mode=local strategy=baseline' "$TMP_DIR/stdout"
grep -q 'k6 command: docker run' "$TMP_DIR/stdout"
grep -q 'args=run --rm -i' "$TMP_DIR/stdout"
grep -q 'line-001' "$TMP_DIR/stdout"
grep -q 'line-012' "$TMP_DIR/stdout"
grep -q 'STRATEGY=baseline' "$TMP_DIR/stdout"
grep -q -- '--summary-mode=full' "$TMP_DIR/stdout"
grep -q -- '--summary-trend-stats avg,min,med,max,p(90),p(95),p(99)' "$TMP_DIR/stdout"
grep -q -- '--summary-time-unit ms' "$TMP_DIR/stdout"
grep -q -- '--summary-export /scripts/results/' "$TMP_DIR/stdout"

LOG_FILE="$TMP_DIR/k6/results/products-baseline-local.log"
if [[ -f "$LOG_FILE" ]]; then
  echo "expected default k6 run to avoid writing a log file" >&2
  exit 1
fi

set +e
PATH="$TMP_DIR/bin:$PATH" K6_LOG_FILE="$LOG_FILE" K6_TAIL_ONLY=1 K6_TAIL_LINES=5 "$TMP_DIR/k6/run.sh" products baseline local > "$TMP_DIR/tail-stdout" 2> "$TMP_DIR/tail-stderr"
STATUS=$?
set -e

if [[ "$STATUS" -ne 7 ]]; then
  echo "expected tail-only k6 exit status 7, got $STATUS" >&2
  exit 1
fi

if grep -q 'line-001' "$TMP_DIR/tail-stdout"; then
  echo "expected tail-only stdout to omit early k6 output" >&2
  exit 1
fi

grep -q 'line-008' "$TMP_DIR/tail-stdout"
grep -q 'line-012' "$TMP_DIR/tail-stdout"
grep -q 'line-001' "$LOG_FILE"
grep -q 'line-012' "$LOG_FILE"
grep -q 'args=run --rm -i' "$LOG_FILE"
grep -q -- '--summary-mode=full' "$LOG_FILE"
grep -q -- '--summary-trend-stats avg,min,med,max,p(90),p(95),p(99)' "$LOG_FILE"
grep -q -- '--summary-time-unit ms' "$LOG_FILE"
grep -q -- '--summary-export /scripts/results/' "$LOG_FILE"
grep -q 'STRATEGY=baseline' "$LOG_FILE"

cat > "$TMP_DIR/bin/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
if [[ "${MSYS_NO_PATHCONV:-}" != "1" ]]; then
  echo "missing MSYS_NO_PATHCONV" >&2
  exit 23
fi

last_arg="${@: -1}"
printf 'args=%s\n' "$*"
for i in $(seq 1 6); do
  printf 'compose-line-%03d\n' "$i"
done
printf 'last-arg=%s\n' "$last_arg"
exit 9
FAKE_DOCKER
chmod +x "$TMP_DIR/bin/docker"

set +e
PATH="$TMP_DIR/bin:$PATH" K6_LOG_FILE="$TMP_DIR/k6/results/products-baseline-prometheus.log" K6_TAIL_ONLY=1 K6_TAIL_LINES=3 "$TMP_DIR/k6/run.sh" products baseline prometheus > "$TMP_DIR/prometheus-stdout" 2> "$TMP_DIR/prometheus-stderr"
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
grep -q -- '--summary-mode=full' "$PROMETHEUS_LOG_FILE"
grep -q -- '--summary-trend-stats avg,min,med,max,p(90),p(95),p(99)' "$PROMETHEUS_LOG_FILE"
grep -q -- '--summary-time-unit ms' "$PROMETHEUS_LOG_FILE"
grep -q -- '--summary-export /scripts/results/' "$PROMETHEUS_LOG_FILE"

cat > "$TMP_DIR/bin/docker" <<'FAKE_DOCKER_SUMMARY'
#!/usr/bin/env bash
summary_export=""
while [[ "$#" -gt 0 ]]; do
  if [[ "$1" == "--summary-export" ]]; then
    shift
    summary_export="$1"
    break
  fi
  shift
done

if [[ -z "$summary_export" ]]; then
  echo "missing summary export" >&2
  exit 24
fi

if [[ "$summary_export" == /scripts/results/* ]]; then
  summary_export="results/${summary_export#/scripts/results/}"
fi

mkdir -p "$(dirname "$summary_export")"
printf '{"metrics":{"http_req_duration":{"values":{"p(99)":123}}}}\n' > "$summary_export"
echo "summary-export=$summary_export"
exit 99
FAKE_DOCKER_SUMMARY
chmod +x "$TMP_DIR/bin/docker"

SUMMARY_JSON_FILE="$TMP_DIR/evidence/products/k6-summary.json"

set +e
PATH="$TMP_DIR/bin:$PATH" K6_SUMMARY_JSON_FILE="$SUMMARY_JSON_FILE" K6_LOG_FILE="$TMP_DIR/summary-output.txt" K6_TAIL_ONLY=1 K6_TAIL_LINES=3 "$TMP_DIR/k6/run.sh" products baseline local > "$TMP_DIR/summary-stdout" 2> "$TMP_DIR/summary-stderr"
STATUS=$?
set -e

if [[ "$STATUS" -ne 99 ]]; then
  echo "expected summary k6 exit status 99, got $STATUS" >&2
  exit 1
fi

grep -q '"p(99)":123' "$SUMMARY_JSON_FILE"
grep -q "k6 summary json: $SUMMARY_JSON_FILE" "$TMP_DIR/summary-stdout"

cat > "$TMP_DIR/bin/docker" <<'FAKE_DOCKER_FAILED'
#!/usr/bin/env bash
exit 1
FAKE_DOCKER_FAILED
chmod +x "$TMP_DIR/bin/docker"

FAILED_RUN_WINDOW_FILE="$TMP_DIR/failed/run-window.json"
FAILED_EXIT_STATUS_FILE="$TMP_DIR/failed/k6-exit-status.txt"

set +e
PATH="$TMP_DIR/bin:$PATH" \
K6_SUMMARY_JSON_FILE=0 \
K6_RUN_WINDOW_FILE="$FAILED_RUN_WINDOW_FILE" \
K6_EXIT_STATUS_FILE="$FAILED_EXIT_STATUS_FILE" \
K6_WRITE_RUN_WINDOW_ON_FAILURE=1 \
"$TMP_DIR/k6/run.sh" products baseline local > "$TMP_DIR/failed-stdout" 2> "$TMP_DIR/failed-stderr"
STATUS=$?
set -e

if [[ "$STATUS" -ne 1 ]]; then
  echo "expected failed k6 exit status 1, got $STATUS" >&2
  exit 1
fi

grep -q '^1$' "$FAILED_EXIT_STATUS_FILE"
grep -q '"resultStatus": "execution_failed"' "$FAILED_RUN_WINDOW_FILE"
grep -q "k6 run window: $FAILED_RUN_WINDOW_FILE" "$TMP_DIR/failed-stdout"

cat > "$TMP_DIR/bin/docker" <<'FAKE_DOCKER_PROGRESS'
#!/usr/bin/env bash
sleep 2
exit 0
FAKE_DOCKER_PROGRESS
chmod +x "$TMP_DIR/bin/docker"

set +e
PATH="$TMP_DIR/bin:$PATH" \
K6_SUMMARY_JSON_FILE=0 \
K6_RUN_WINDOW_FILE=0 \
K6_EXIT_STATUS_FILE=0 \
"$TMP_DIR/k6/run.sh" products baseline local > "$TMP_DIR/progress-stdout" 2> "$TMP_DIR/progress-stderr"
STATUS=$?
set -e

if [[ "$STATUS" -ne 0 ]]; then
  echo "expected progress k6 exit status 0, got $STATUS" >&2
  exit 1
fi

grep -q 'k6 progress: elapsed=1s phase=phase-01 scenario=products preset=baseline pool=pool10 mode=local strategy=baseline' "$TMP_DIR/progress-stdout"

grep -q "__ENV.STRATEGY || 'baseline'" "$REPO_ROOT/k6/products-test.js"
grep -q 'strategy=${STRATEGY}' "$REPO_ROOT/k6/products-test.js"
