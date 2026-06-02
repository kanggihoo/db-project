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
