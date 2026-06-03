.PHONY: db-start db-shell

db-start:
	docker compose up -d postgres prometheus grafana

db-shell:
	docker compose exec postgres psql -U app -d ecommerce
