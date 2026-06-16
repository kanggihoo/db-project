.PHONY: k6-evidence

k6-evidence:
	$(call require-variable,PHASE)
	$(call require-variable,SCENARIO)
	$(call require-variable,CONDITION)
	$(call require-variable,K6_PRESET)
	npm run k6:evidence -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(K6_PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION)
