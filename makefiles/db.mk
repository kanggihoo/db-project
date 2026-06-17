.PHONY: db-start db-shell seed-state

db-start:
	docker compose up -d postgres prometheus grafana

db-shell:
	docker compose exec postgres psql -U app -d ecommerce

seed-state:
	$(call require-variable,SEED_PRESET)
	./scripts/seed.sh $(SEED_PRESET)
	npm run phase:sql -- --file "$(SEED_STATE_SQL)" --output "$(SEED_STATE_OUTPUT)"
