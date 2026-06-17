.PHONY: grafana-generate grafana-capture

grafana-generate:
	node scripts/generate-db-lab-dashboard.mjs

grafana-capture:
	$(call require-variable,PHASE)
	$(call require-variable,SCENARIO)
	$(call require-variable,GRAFANA_PRESET)
	npm run grafana:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(GRAFANA_PRESET) --pool $(POOL) $(if $(TABLE),--table $(TABLE),) $(if $(WINDOW_FILE),--window-file $(WINDOW_FILE),) $(if $(OUTPUT),--output $(OUTPUT),) $(GRAFANA_PHASE_ALIGN_ARG)
