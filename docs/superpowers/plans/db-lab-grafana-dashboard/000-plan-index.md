# DB Lab Grafana Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provision a reusable `DB Lab Overview` Grafana dashboard and k6 label policy so each Learning Phase can capture comparable evidence.

**Architecture:** Keep one shared Grafana dashboard with common rows and Phase focus rows. Add low-cardinality k6 labels through environment variables, use Prometheus as the only Grafana datasource, and keep query-level SQL evidence in `pg_stat_statements` snapshots instead of Prometheus labels.

**Tech Stack:** Bash, k6 JavaScript, Spring Boot Actuator/Micrometer, Prometheus, Grafana provisioning JSON/YAML, Node.js verification scripts.

---

## Spec

Implement [DB Lab Grafana Dashboard Spec](../../specs/db-lab-grafana-dashboard-spec.md).

## Vertical Slices

Run the slices in order. Each slice leaves the repository in a reviewable state.

| Slice | Document | Outcome |
|---|---|---|
| 001 | [First Vertical Slice](./001-first-vertical-slice.md) | Verification script plus k6 measurement-condition labels |
| 002 | [Second Vertical Slice](./002-second-vertical-slice.md) | Stable Prometheus datasource, Spring histograms, generated Grafana dashboard |
| 003 | [Third Vertical Slice](./003-third-vertical-slice.md) | User-facing guides updated for the dashboard workflow |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | JSON, provisioning, Prometheus targets, and final docs verification |

## File Ownership

| Path | Responsibility |
|---|---|
| `scripts/verify-observability.mjs` | Fast repository-level verification for k6 labels, dashboard provisioning, and docs |
| `scripts/generate-db-lab-dashboard.mjs` | Generates deterministic Grafana dashboard JSON |
| `k6/run.sh` | Passes `PHASE`, `SCENARIO`, `PRESET_NAME`, and `POOL` to k6 |
| `k6/orders-test.js` | Adds stable low-cardinality tags for the orders scenario |
| `k6/products-test.js` | Adds stable low-cardinality tags for the products scenario |
| `k6/points-test.js` | Adds stable low-cardinality tags for the points scenario |
| `docker/grafana/provisioning/datasources/prometheus.yml` | Gives the Prometheus datasource a stable UID |
| `docker/grafana/dashboards/db-lab-overview.json` | Provisioned dashboard generated from the script |
| `ecommerce/src/main/resources/application.yaml` | Enables histogram buckets needed for Spring p95 panels |
| `docs/guides/grafana-observability.md` | Links the dashboard spec and usage model |
| `docs/guides/k6-load-testing.md` | Documents labeled k6 execution |
| `docs/guides/environment.md` | Documents dashboard provisioning and first verification |

## Cross-Slice Invariants

- Dashboard title: `DB Lab Overview`
- Dashboard UID: `db-lab-overview`
- Datasource UID: `prometheus`
- k6 labels: `phase`, `scenario`, `preset`, `pool`
- k6 request names: `GET /api/orders`, `GET /api/products`, `GET /api/points`
- Spec path: `docs/superpowers/specs/db-lab-grafana-dashboard-spec.md`

## Verification Policy

Every slice must run its local verification command before commit. The stabilization slice must run the full runtime checks against Docker Compose and Grafana API.
