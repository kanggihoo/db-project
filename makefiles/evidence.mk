.PHONY: evidence-capture phase-status

evidence-capture:
	$(call require-variable,PHASE)
	$(call require-variable,SCENARIO)
	$(call require-variable,CONDITION)
	$(call require-variable,K6_PRESET)
	npm run evidence:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(K6_PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION) $(if $(TABLE),--table $(TABLE),) $(if $(OUTPUT),--output $(OUTPUT),)

phase-status:
	$(call require-variable,PHASE)
	@echo "Phase: $(PHASE)"
	@find docs/evidence/$(PHASE) -maxdepth 3 -type f 2>/dev/null | sort || true
