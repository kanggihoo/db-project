.PHONY: k6-evidence

k6-evidence:
	npm run k6:evidence -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(K6_PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION)
