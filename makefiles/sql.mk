.PHONY: phase-sql

phase-sql:
	npm run phase:sql -- --phase $(PHASE) --scenario $(SCENARIO) $(PHASE_SQL_CONDITION_ARG) --action $(ACTION) $(if $(OUTPUT),--output $(OUTPUT),)
