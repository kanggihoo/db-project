# Commands

루트 `Makefile`이 공개 명령 인터페이스다. 명령 wrapper 자체를 개발하는 경우가 아니면 `package.json` scripts와 `scripts/` 아래 파일은 구현 세부사항으로 본다.

## Common Variables

| Variable | Example | Meaning |
|---|---|---|
| `PHASE` | `phase-06` | Phase id |
| `SCENARIO` | `review-summary` | k6/Grafana scenario |
| `CONDITION` | `query-shaped-index` | Evidence condition |
| `FILE` | `scripts/phase-06/13-review-naive-explain.sql` | SQL runner input file |
| `SEED_PRESET` | `loadtest` | Seed preset for common seed-state evidence |
| `PRESET` | `review-summary-baseline` | k6/Grafana preset |
| `K6_PRESET` | `review-summary-baseline` | k6 preset override, defaults to `PRESET` |
| `GRAFANA_PRESET` | `baseline` | Grafana preset override, defaults to `PRESET` |
| `POOL` | `pool10` | Connection pool label |
| `MODE` | `prometheus` | k6 execution mode |
| `TABLE` | `review` | Grafana table variable |
| `OUTPUT` | `docs/evidence/phase-06/.../explain.txt` | Output file path |
| `WINDOW_FILE` | `docs/evidence/phase-06/.../run-window.json` | Fixed Grafana capture window |

## Phase 6 Examples

```bash
make seed-state SEED_PRESET=loadtest
make phase-sql FILE=scripts/phase-06/10-review-baseline-prepare.sql
make phase-sql FILE=scripts/phase-06/11-review-baseline-explain.sql OUTPUT=docs/evidence/phase-06/review-aggregate/baseline/explain.txt
make k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index
make grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=baseline TABLE=review
make grafana-capture PHASE=phase-06 SCENARIO=review-summary GRAFANA_PRESET=baseline TABLE=review WINDOW_FILE=docs/evidence/phase-06/review-summary-api/query-shaped-index/run-window.json
```
