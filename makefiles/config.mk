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
