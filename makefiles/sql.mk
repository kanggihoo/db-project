.PHONY: phase-sql

phase-sql:
	$(call require-variable,FILE)
	npm run phase:sql -- --file "$(FILE)" $(if $(OUTPUT),--output "$(OUTPUT)",)
