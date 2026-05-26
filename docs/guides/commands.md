# Commands

루트 `Makefile`이 공개 명령 인터페이스다. 명령 wrapper 자체를 개발하는 경우가 아니면 `package.json` scripts와 `scripts/` 아래 파일은 구현 세부사항으로 본다.

## Common Variables

| Variable | Example | Meaning |
|---|---|---|
| `PHASE` | `phase-06` | Phase id |
| `SCENARIO` | `review-aggregate` | SQL 또는 k6 scenario |
| `CONDITION` | `query-shaped-index` | Evidence condition |
| `ACTION` | `prepare` | SQL runner action |
| `PRESET` | `review-summary-baseline` | k6 preset |
| `POOL` | `pool10` | Connection pool label |
| `TABLE` | `review` | Grafana table variable |
| `OUTPUT` | `docs/evidence/phase-06/.../explain.txt` | Output file path |

## Phase 6 Examples

```bash
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=baseline ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=baseline ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/baseline/explain.txt
make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index
make grafana-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index TABLE=review
```
