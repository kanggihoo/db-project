# 006 Standard Command Interface

## Goal

Introduce a root `Makefile` so Phase 6 and later phases can use the same command format for SQL evidence, k6, Grafana capture, and project status checks.

## Files

- Create: `Makefile`
- Create: `scripts/run-phase-sql.mjs`
- Modify: `package.json`
- Create: `docs/guides/commands.md`
- Modify: `docs/guides/scripts.md`
- Modify: `docs/phases/06-aggregation/runbook.md`
- Modify: `docs/phases/06-aggregation/observability.md`

## Steps

- [ ] **Step 1: Create the phase SQL runner**

Create `scripts/run-phase-sql.mjs` with this responsibility: map standard variables to numbered Phase 6 SQL files, run `docker compose exec -T postgres psql`, and write stdout to `OUTPUT` when provided.

Use this mapping:

```js
const PHASE_06_SCRIPTS = {
  'data-profile:profile': 'scripts/phase-06/00-data-profile.sql',
  'review-aggregate:baseline:prepare': 'scripts/phase-06/10-review-baseline-prepare.sql',
  'review-aggregate:baseline:explain': 'scripts/phase-06/11-review-baseline-explain.sql',
  'review-aggregate:naive-index:prepare': 'scripts/phase-06/12-review-naive-prepare.sql',
  'review-aggregate:naive-index:explain': 'scripts/phase-06/13-review-naive-explain.sql',
  'review-aggregate:query-shaped-index:prepare': 'scripts/phase-06/14-review-query-shaped-prepare.sql',
  'review-aggregate:query-shaped-index:explain': 'scripts/phase-06/15-review-query-shaped-explain.sql',
  'monthly-order-aggregate:baseline:prepare': 'scripts/phase-06/20-monthly-baseline-prepare.sql',
  'monthly-order-aggregate:baseline:explain': 'scripts/phase-06/21-monthly-baseline-explain.sql',
  'monthly-order-aggregate:naive-index:prepare': 'scripts/phase-06/22-monthly-naive-prepare.sql',
  'monthly-order-aggregate:naive-index:explain': 'scripts/phase-06/23-monthly-naive-explain.sql',
  'monthly-order-aggregate:query-shaped-index:prepare': 'scripts/phase-06/24-monthly-query-shaped-prepare.sql',
  'monthly-order-aggregate:query-shaped-index:explain': 'scripts/phase-06/25-monthly-query-shaped-explain.sql',
};
```

The runner must accept:

```text
--phase phase-06
--scenario review-aggregate
--condition naive-index
--action prepare
--output docs/evidence/phase-06/review-aggregate/naive-index/explain.txt
```

Expected behavior:

- Reject any phase other than `phase-06` with a clear message until later phases add mappings.
- Require `--action profile` for `data-profile`.
- Require `--condition` and `--action prepare|explain` for aggregation scenarios.
- Create the output directory before writing `--output`.
- Use `docker compose exec -T postgres psql -U app -d ecommerce -f <script>`.

- [ ] **Step 2: Add package script for the phase SQL runner**

Modify `package.json`:

```json
"phase:sql": "node scripts/run-phase-sql.mjs"
```

Expected: `npm run phase:sql -- --phase phase-06 --scenario data-profile --action profile` runs the runner.

- [ ] **Step 3: Create the root Makefile**

Create `Makefile` as a thin facade. Keep command bodies short and delegate to `npm`, `node`, `docker compose`, or existing scripts.

```makefile
.PHONY: help env-check db-start db-shell grafana-generate grafana-capture k6-evidence evidence-capture phase-sql phase-status

PHASE ?= phase-06
SCENARIO ?= review-summary
PRESET ?= review-summary-baseline
MODE ?= prometheus
POOL ?= pool10
PROFILE ?= local
CONDITION ?= naive-index
ACTION ?= explain
TABLE ?=
OUTPUT ?=
WINDOW_FILE ?=
TAIL ?= 120

help:
	@echo "Available targets:"
	@echo "  make env-check"
	@echo "  make db-start"
	@echo "  make db-shell"
	@echo "  make grafana-generate"
	@echo "  make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare"
	@echo "  make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/naive-index/explain.txt"
	@echo "  make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index"
	@echo "  make grafana-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index TABLE=review"
	@echo "  make evidence-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index TABLE=review"
	@echo "  make phase-status PHASE=phase-06"

env-check:
	@docker --version
	@docker compose version
	@node --version
	@npm --version
	@k6 version

db-start:
	docker compose up -d postgres prometheus grafana

db-shell:
	docker compose exec postgres psql -U app -d ecommerce

grafana-generate:
	node scripts/generate-db-lab-dashboard.mjs

phase-sql:
	npm run phase:sql -- --phase $(PHASE) --scenario $(SCENARIO) --condition $(CONDITION) --action $(ACTION) $(if $(OUTPUT),--output $(OUTPUT),)

k6-evidence:
	npm run k6:evidence -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION)

grafana-capture:
	npm run grafana:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(PRESET) --pool $(POOL) $(if $(TABLE),--table $(TABLE),) $(if $(WINDOW_FILE),--window-file $(WINDOW_FILE),) $(if $(OUTPUT),--output $(OUTPUT),)

evidence-capture:
	npm run evidence:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION) $(if $(TABLE),--table $(TABLE),) $(if $(OUTPUT),--output $(OUTPUT),)

phase-status:
	@echo "Phase: $(PHASE)"
	@find docs/evidence/$(PHASE) -maxdepth 3 -type f 2>/dev/null | sort || true
```

Expected: `make help` shows the supported target names and example commands.

- [ ] **Step 4: Add command guide**

Create `docs/guides/commands.md` with these sections:

```markdown
# Commands

The root `Makefile` is the public command interface. Use `package.json` scripts and files under `scripts/` as implementation details unless developing the command wrappers themselves.

## Common Variables

| Variable | Example | Meaning |
|---|---|---|
| `PHASE` | `phase-06` | Phase id |
| `SCENARIO` | `review-aggregate` | SQL or k6 scenario |
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
```

Expected: users can find the standard command format without reading `scripts/` first.

- [ ] **Step 5: Point script guide at Makefile for repeated tasks**

Modify `docs/guides/scripts.md` near the top:

```markdown
For repeated project tasks, prefer the root `Makefile` and `docs/guides/commands.md`. This page documents the implementation scripts that Makefile targets call.
```

Expected: command discovery starts at `make help` and `docs/guides/commands.md`.

- [ ] **Step 6: Update Phase 6 runbook examples**

Modify `docs/phases/06-aggregation/runbook.md` so SQL and k6 examples use Makefile commands:

```bash
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/naive-index/explain.txt
make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index
```

Expected: the runbook does not require users to assemble long `docker compose exec` or k6 commands.

- [ ] **Step 7: Update Phase 6 observability examples**

Modify `docs/phases/06-aggregation/observability.md` so Grafana examples use Makefile commands:

```bash
make grafana-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index TABLE=review WINDOW_FILE=docs/evidence/phase-06/review-summary-api/naive-index/run-window.json OUTPUT=docs/evidence/phase-06/grafana-screenshots/review-summary-naive-index.png
```

Expected: screenshots can be reproduced with the same variable names used by SQL and k6 commands.

- [ ] **Step 8: Verify command interface**

Run:

```bash
rtk make help
rtk npm run phase:sql -- --phase phase-06 --scenario data-profile --action profile --output docs/evidence/phase-06/data-profile/row-counts.txt
rtk rg -n "make phase-sql|make k6-evidence|make grafana-capture|Makefile" Makefile docs/guides/commands.md docs/guides/scripts.md docs/phases/06-aggregation
```

Expected: `make help` exits 0, the SQL runner can invoke the data profile script, and docs use the standard command examples.

- [ ] **Step 9: Commit**

```bash
git add Makefile package.json scripts/run-phase-sql.mjs docs/guides/commands.md docs/guides/scripts.md docs/phases/06-aggregation/runbook.md docs/phases/06-aggregation/observability.md
git commit -m "chore: add standard project command interface"
```

