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

PHASE_SQL_CONDITION_ARG = $(if $(filter data-profile,$(SCENARIO)),,--condition $(CONDITION))
GRAFANA_PHASE_ALIGN_ARG = $(if $(filter phase-06,$(PHASE)),--no-align-phase-rows,)

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
	@echo "  grafana-capture: PHASE, SCENARIO, WINDOW_FILE for fixed evidence captures"
	@echo ""
	@echo "Default output roots:"
	@echo "  SQL/k6 evidence: docs/evidence/<phase>/"
	@echo "  Grafana screenshots: docs/evidence/<phase>/grafana-screenshots/"

env-check:
	@docker --version
	@docker compose version
	@node --version
	@npm --version
	@if command -v k6 >/dev/null 2>&1; then k6 version; else docker image inspect grafana/k6 --format 'grafana/k6 {{.Id}}' >/dev/null; fi

db-start:
	docker compose up -d postgres prometheus grafana

db-shell:
	docker compose exec postgres psql -U app -d ecommerce

grafana-generate:
	node scripts/generate-db-lab-dashboard.mjs

phase-sql:
	npm run phase:sql -- --phase $(PHASE) --scenario $(SCENARIO) $(PHASE_SQL_CONDITION_ARG) --action $(ACTION) $(if $(OUTPUT),--output $(OUTPUT),)

k6-evidence:
	npm run k6:evidence -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION)

grafana-capture:
	npm run grafana:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(PRESET) --pool $(POOL) $(if $(TABLE),--table $(TABLE),) $(if $(WINDOW_FILE),--window-file $(WINDOW_FILE),) $(if $(OUTPUT),--output $(OUTPUT),) $(GRAFANA_PHASE_ALIGN_ARG)

evidence-capture:
	npm run evidence:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION) $(if $(TABLE),--table $(TABLE),) $(if $(OUTPUT),--output $(OUTPUT),)

phase-status:
	@echo "Phase: $(PHASE)"
	@find docs/evidence/$(PHASE) -maxdepth 3 -type f 2>/dev/null | sort || true
