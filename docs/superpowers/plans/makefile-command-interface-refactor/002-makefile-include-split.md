# 002 Makefile Include Split

## Goal

현재 동작을 유지하면서 루트 `Makefile`을 include 중심으로 축소하고 target 구현을 `makefiles/*.mk`로 이동한다.

## Files

- Modify: `Makefile`
- Create: `makefiles/config.mk`
- Create: `makefiles/help.mk`
- Create: `makefiles/env.mk`
- Create: `makefiles/db.mk`
- Create: `makefiles/grafana.mk`
- Create: `makefiles/k6.mk`
- Create: `makefiles/evidence.mk`
- Create: `makefiles/sql.mk`

## Steps

- [ ] **Step 1: Create `makefiles/config.mk`**

Create:

```make
PHASE ?= phase-06
SCENARIO ?= review-summary
PRESET ?= review-summary-baseline
K6_PRESET ?= $(PRESET)
GRAFANA_PRESET ?= $(PRESET)
MODE ?= prometheus
POOL ?= pool10
PROFILE ?= local
CONDITION ?= naive-index
ACTION ?= explain
TABLE ?=
OUTPUT ?=
WINDOW_FILE ?=
TAIL ?= 120

PHASE_SQL_CONDITION_ARG = $(if $(filter data-profile,$(SCENARIO)),,--condition $(CONDITION))
GRAFANA_PHASE_ALIGN_ARG = $(if $(filter phase-06,$(PHASE)),--no-align-phase-rows,)
```

- [ ] **Step 2: Create `makefiles/help.mk`**

Create:

```make
.PHONY: help

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
	@echo ""
	@echo "Common required variables:"
	@echo "  phase-sql: PHASE, SCENARIO, ACTION, and CONDITION for aggregation scenarios"
	@echo "  k6-evidence/evidence-capture: PHASE, SCENARIO, CONDITION"
	@echo "  grafana-capture: PHASE, SCENARIO, optional WINDOW_FILE for fixed evidence captures"
	@echo ""
	@echo "Preset variables:"
	@echo "  PRESET: public default for k6 and Grafana"
	@echo "  K6_PRESET: k6 preset override, defaults to PRESET"
	@echo "  GRAFANA_PRESET: Grafana preset variable override, defaults to PRESET"
	@echo ""
	@echo "Default output roots:"
	@echo "  SQL/k6 evidence: docs/evidence/<phase>/"
	@echo "  Grafana screenshots: docs/evidence/<phase>/grafana-screenshots/"
```

- [ ] **Step 3: Create `makefiles/env.mk`**

Create:

```make
.PHONY: env-check

env-check:
	@docker --version
	@docker compose version
	@node --version
	@npm --version
	@if command -v k6 >/dev/null 2>&1; then k6 version; else docker image inspect grafana/k6 --format 'grafana/k6 {{.Id}}' >/dev/null; fi
```

- [ ] **Step 4: Create `makefiles/db.mk`**

Create:

```make
.PHONY: db-start db-shell

db-start:
	docker compose up -d postgres prometheus grafana

db-shell:
	docker compose exec postgres psql -U app -d ecommerce
```

- [ ] **Step 5: Create `makefiles/grafana.mk`**

Create:

```make
.PHONY: grafana-generate grafana-capture

grafana-generate:
	node scripts/generate-db-lab-dashboard.mjs

grafana-capture:
	npm run grafana:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(GRAFANA_PRESET) --pool $(POOL) $(if $(TABLE),--table $(TABLE),) $(if $(WINDOW_FILE),--window-file $(WINDOW_FILE),) $(if $(OUTPUT),--output $(OUTPUT),) $(GRAFANA_PHASE_ALIGN_ARG)
```

- [ ] **Step 6: Create `makefiles/k6.mk`**

Create:

```make
.PHONY: k6-evidence

k6-evidence:
	npm run k6:evidence -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(K6_PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION)
```

- [ ] **Step 7: Create `makefiles/evidence.mk`**

Create:

```make
.PHONY: evidence-capture phase-status

evidence-capture:
	npm run evidence:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(K6_PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION) $(if $(TABLE),--table $(TABLE),) $(if $(OUTPUT),--output $(OUTPUT),)

phase-status:
	@echo "Phase: $(PHASE)"
	@find docs/evidence/$(PHASE) -maxdepth 3 -type f 2>/dev/null | sort || true
```

- [ ] **Step 8: Create `makefiles/sql.mk`**

Create:

```make
.PHONY: phase-sql

phase-sql:
	npm run phase:sql -- --phase $(PHASE) --scenario $(SCENARIO) $(PHASE_SQL_CONDITION_ARG) --action $(ACTION) $(if $(OUTPUT),--output $(OUTPUT),)
```

- [ ] **Step 9: Replace root `Makefile` with include entrypoint**

Modify `Makefile` to:

```make
SHELL := /bin/bash
.DEFAULT_GOAL := help

include makefiles/config.mk
include makefiles/help.mk
include makefiles/env.mk
include makefiles/db.mk
include makefiles/grafana.mk
include makefiles/k6.mk
include makefiles/evidence.mk
include makefiles/sql.mk
```

- [ ] **Step 10: Verify help still works**

Run:

```bash
rtk proxy make help
```

Expected:

- command exits 0
- output includes the same public targets
- output mentions `K6_PRESET` and `GRAFANA_PRESET`

- [ ] **Step 11: Verify baseline dry-runs still match**

Run:

```bash
rtk proxy make -n phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index TABLE=review
rtk proxy make -n evidence-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=query-shaped-index TABLE=review
```

Expected:

- command expansions are equivalent to the baseline
- `k6-evidence` and `evidence-capture` read preset through `K6_PRESET`
- `grafana-capture` reads preset through `GRAFANA_PRESET`

- [ ] **Step 12: Commit**

```bash
git add Makefile makefiles
git commit -m "refactor(make): split command interface by responsibility"
```
