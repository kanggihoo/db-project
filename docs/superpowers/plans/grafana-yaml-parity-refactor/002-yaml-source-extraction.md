# 002 YAML Source Extraction

## Goal

현재 `scripts/generate-db-lab-dashboard.mjs`에 하드코딩된 dashboard metadata, variables, rows, panels, PromQL을 YAML source로 옮긴다.

## Files

- Create: `scripts/grafana/dashboards/db-lab-overview.yml`
- Create: `scripts/grafana/rows/run-summary.yml`
- Create: `scripts/grafana/rows/k6-load.yml`
- Create: `scripts/grafana/rows/spring-api.yml`
- Create: `scripts/grafana/rows/spring-runtime.yml`
- Create: `scripts/grafana/rows/hikari-pool.yml`
- Create: `scripts/grafana/rows/postgres-activity.yml`
- Create: `scripts/grafana/rows/table-access.yml`
- Create: `scripts/grafana/rows/phase-focus.yml`
- Create: `scripts/grafana/queries/k6.yml`
- Create: `scripts/grafana/queries/spring.yml`
- Create: `scripts/grafana/queries/hikari.yml`
- Create: `scripts/grafana/queries/postgres.yml`

## Steps

- [ ] **Step 1: Create dashboard YAML**

Create `scripts/grafana/dashboards/db-lab-overview.yml`.

Required fields:

```yaml
uid: db-lab-overview
title: DB Lab Overview
output: docker/grafana/dashboards/db-lab-overview.json
tags:
  - db-lab
  - ecommerce
  - observability
```

Variables must include:

- `phase`
- `scenario`
- `preset`
- `pool`
- `uri`
- `table`

Rows must include existing row groups in the same order:

- `run-summary`
- `k6-load`
- `spring-api`
- `spring-runtime`
- `hikari-pool`
- `postgres-activity`
- `table-access`
- `phase-focus`

- [ ] **Step 2: Extract k6 queries**

Create `scripts/grafana/queries/k6.yml`.

Move existing k6 PromQL expressions from `scripts/generate-db-lab-dashboard.mjs` into aliases, including:

- k6 p95
- k6 p99
- actual RPS
- error rate
- checks success
- dropped iterations
- VUs
- success/error request rate
- iteration p95

Use existing label filter:

```text
phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"
```

- [ ] **Step 3: Extract Spring queries**

Create `scripts/grafana/queries/spring.yml`.

Move existing Spring API/runtime PromQL expressions, including:

- HTTP request rate by URI
- HTTP p95 by URI
- HTTP errors by status
- heap used
- process CPU
- GC pause

Keep existing URI filter behavior:

```text
uri=~"$uri", uri!="/actuator/prometheus", uri!="/**"
```

- [ ] **Step 4: Extract Hikari queries**

Create `scripts/grafana/queries/hikari.yml`.

Move existing Hikari PromQL expressions, including:

- timeout count
- active connections
- max connections
- pending threads
- acquire time

- [ ] **Step 5: Extract PostgreSQL queries**

Create `scripts/grafana/queries/postgres.yml`.

Move existing PostgreSQL PromQL expressions, including:

- PG connections used
- active sessions
- locks
- commit rate
- rollback rate
- table seq scan
- table index scan
- seq tuples read rate
- index tuples fetch rate

- [ ] **Step 6: Create row YAML files**

Create one row YAML per current dashboard section.

Each panel should specify at least:

```yaml
type: stat | timeseries | table
title: <existing panel title>
query: <query alias>
```

Add layout overrides where needed to preserve current visual layout:

```yaml
layout:
  x: 0
  yOffset: 0
  w: 6
  h: 4
```

- [ ] **Step 7: Represent phase focus rows**

Create `scripts/grafana/rows/phase-focus.yml`.

This file should represent the existing generated focus rows:

- `Phase 1 Baseline Focus`
- `Phase 2 Index Focus`
- `Phase 3 N+1 Focus`
- `Phase 4 Transaction Focus`
- `Phase 7 Pagination Focus`

Do not add, remove, or redesign focus rows in this slice.

- [ ] **Step 8: Commit**

```bash
git add scripts/grafana/dashboards scripts/grafana/rows scripts/grafana/queries
git commit -m "chore(grafana): extract dashboard yaml sources"
```
