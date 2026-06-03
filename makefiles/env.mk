.PHONY: env-check

env-check:
	@docker --version
	@docker compose version
	@node --version
	@npm --version
	@if command -v k6 >/dev/null 2>&1; then k6 version; else docker image inspect grafana/k6 --format 'grafana/k6 {{.Id}}' >/dev/null; fi
