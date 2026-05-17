# 999 Integration Stabilization

## Goal

Verify the dashboard can be generated, provisioned, and discovered through Grafana after all vertical slices are complete.

## Files

- No planned source changes.
- Modify only the file that fails a verification step, then rerun the same step.

## Steps

- [ ] **Step 1: Validate generated dashboard JSON**

Run:

```bash
rtk proxy node -e "JSON.parse(require('node:fs').readFileSync('docker/grafana/dashboards/db-lab-overview.json','utf8')); console.log('dashboard json ok')"
```

Expected: `dashboard json ok`.

- [ ] **Step 2: Start infrastructure**

Run:

```bash
rtk docker compose up -d
rtk docker compose ps
```

Expected: `postgres`, `postgres_exporter`, `prometheus`, and `grafana` are `Up`.

- [ ] **Step 3: Verify Grafana datasource provisioning**

Run:

```bash
rtk curl -s -u admin:admin http://localhost:3000/api/datasources
```

Expected: response contains:

```json
"uid":"prometheus"
```

- [ ] **Step 4: Verify dashboard provisioning**

Run:

```bash
rtk curl -s -u admin:admin 'http://localhost:3000/api/search?query=DB%20Lab%20Overview'
```

Expected: response contains:

```json
"title":"DB Lab Overview"
```

- [ ] **Step 5: Verify Prometheus targets**

Run:

```bash
rtk curl -s http://localhost:9090/api/v1/targets
```

Expected:

- `postgres_exporter:9187` target is `up`.
- `host.docker.internal:8080` may be `down` if Spring Boot is not running.

- [ ] **Step 6: Verify repository observability checks**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: PASS with:

```text
Observability configuration verified.
```

- [ ] **Step 7: Verify docs do not point at the old spec path**

Run:

```bash
rtk proxy rg -n "docs/guides/db-lab-grafana-dashboard-spec|guides/db-lab-grafana-dashboard-spec|2026-05-17-db-lab-grafana-dashboard.md" docs --glob '!docs/superpowers/plans/db-lab-grafana-dashboard/999-integration-stabilization.md'
```

Expected: zero matches.

- [ ] **Step 8: Commit stabilization fixes if needed**

Only run if this stabilization slice required source changes:

```bash
git add <changed-files>
git commit -m "fix: stabilize db lab grafana dashboard"
```
