# 002 Second Vertical Slice: Provisioned Dashboard

## Goal

Create the generated Grafana dashboard path and make Prometheus/Spring metrics stable enough for reusable panels.

## Files

- Modify: `docker/grafana/provisioning/datasources/prometheus.yml`
- Modify: `ecommerce/src/main/resources/application.yaml`
- Create: `scripts/generate-db-lab-dashboard.mjs`
- Create: `docker/grafana/dashboards/db-lab-overview.json`
- Test: `scripts/verify-observability.mjs`

## Steps

- [ ] **Step 1: Set a stable Prometheus datasource UID**

Replace `docker/grafana/provisioning/datasources/prometheus.yml` with:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

- [ ] **Step 2: Enable Spring histogram buckets**

In `ecommerce/src/main/resources/application.yaml`, make the `management:` block include:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
        hikaricp.connections.acquire: true
```

- [ ] **Step 3: Add the dashboard generator**

Create `scripts/generate-db-lab-dashboard.mjs`. It must generate `docker/grafana/dashboards/db-lab-overview.json` with:

```js
const datasource = {
  type: 'prometheus',
  uid: 'prometheus',
};

const k6Filter = 'phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"';
```

The generated dashboard must include:

| Field | Value |
|---|---|
| `title` | `DB Lab Overview` |
| `uid` | `db-lab-overview` |
| `refresh` | `10s` |
| default time | `now-30m` to `now` |
| tags | `db-lab`, `ecommerce`, `observability` |

It must define these template variables:

```js
[
  'phase',
  'scenario',
  'preset',
  'pool',
  'uri',
  'table',
]
```

It must define these row panels:

```js
[
  'Run Summary',
  'k6 Load',
  'Spring API',
  'Hikari Pool',
  'PostgreSQL Activity',
  'Table Access',
  'Phase 1 Baseline Focus',
  'Phase 2 Index Focus',
  'Phase 3 N+1 Focus',
  'Phase 7 Pagination Focus',
]
```

Use deterministic IDs. Do not use random values for panel ids or target refIds.

- [ ] **Step 4: Add required panel queries**

The generator must include at least these panel titles and PromQL fragments:

| Panel | PromQL fragment |
|---|---|
| `k6 p95` | `histogram_quantile(0.95` and `k6_http_req_duration_seconds` |
| `k6 p99` | `histogram_quantile(0.99` and `k6_http_req_duration_seconds` |
| `Error Rate` | `k6_http_req_failed_rate` |
| `Actual RPS` | `rate(k6_http_reqs_total` |
| `Dropped Iterations` | `k6_dropped_iterations_total` |
| `Hikari Pending Max` | `hikaricp_connections_pending` |
| `HTTP p95 by URI` | `http_server_requests_seconds_bucket` |
| `Seq Scan by Table` | `pg_stat_user_tables_seq_scan` |
| `Index Scan by Table` | `pg_stat_user_tables_idx_scan` |

- [ ] **Step 5: Generate dashboard JSON**

Run:

```bash
rtk proxy node scripts/generate-db-lab-dashboard.mjs
```

Expected: `docker/grafana/dashboards/db-lab-overview.json` exists and is valid JSON.

- [ ] **Step 6: Verify slice**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: FAIL only for missing docs if slice 003 has not been completed. It must not fail for datasource UID, dashboard JSON, or Spring histograms.

- [ ] **Step 7: Commit**

```bash
git add docker/grafana/provisioning/datasources/prometheus.yml ecommerce/src/main/resources/application.yaml scripts/generate-db-lab-dashboard.mjs docker/grafana/dashboards/db-lab-overview.json
git commit -m "feat: provision db lab grafana dashboard"
```
