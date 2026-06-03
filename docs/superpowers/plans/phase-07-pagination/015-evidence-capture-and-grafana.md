# 015 Evidence Capture And Grafana

## Goal

A/B/C 재측정 evidence를 run별로 분리 저장하고, 기존 shared `DB Lab Overview` Grafana screenshot을 보조 evidence로 캡처한다.

## Files

- Output: `docs/evidence/phase-07/k6/retest-offset-sampling-summary.txt`
- Output: `docs/evidence/phase-07/k6/retest-cursor-sampling-summary.txt`
- Output: `docs/evidence/phase-07/pg-stat-statements/retest-offset-sampling-api.txt`
- Output: `docs/evidence/phase-07/pg-stat-statements/retest-cursor-sampling-api.txt`
- Output: `docs/evidence/phase-07/grafana/retest-db-lab-overview.png`

## Steps

- [ ] **Step 1: Reset SQL stats before Offset sampling**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
```

Expected:

```text
 pg_stat_statements_reset
```

- [ ] **Step 2: Run Offset sampling k6**

Run:

```bash
rtk docker compose run --rm -e PHASE=phase-07 -e SCENARIO=points-offset-sampling -e PRESET_NAME=offset-sampling -e PRESET=presets/points-offset-sampling.json k6 run /scripts/points-offset-sampling-test.js
```

Save stdout summary to:

```text
docs/evidence/phase-07/k6/retest-offset-sampling-summary.txt
```

- [ ] **Step 3: Save Offset pg_stat_statements**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/30-pg-stat-statements.sql
```

Save output to:

```text
docs/evidence/phase-07/pg-stat-statements/retest-offset-sampling-api.txt
```

Expected query shapes:

```text
select ... from point_history ... order by ... offset ... fetch first ...
select count(*) from point_history ... where user_id=$1
```

- [ ] **Step 4: Reset SQL stats before Cursor sampling**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
```

- [ ] **Step 5: Run Cursor sampling k6**

Run:

```bash
rtk docker compose run --rm -e PHASE=phase-07 -e SCENARIO=points-cursor-sampling -e PRESET_NAME=cursor-sampling -e PRESET=presets/points-cursor-sampling.json k6 run /scripts/points-cursor-sampling-test.js
```

Save stdout summary to:

```text
docs/evidence/phase-07/k6/retest-cursor-sampling-summary.txt
```

- [ ] **Step 6: Save Cursor pg_stat_statements**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/30-pg-stat-statements.sql
```

Save output to:

```text
docs/evidence/phase-07/pg-stat-statements/retest-cursor-sampling-api.txt
```

Expected query shapes:

```text
select ... from point_history ... order by ... fetch first ...
select ... from point_history ... created_at < ... or ... id < ... order by ... fetch first ...
```

Expected absent query:

```text
select count(*) from point_history
```

- [ ] **Step 7: Capture Grafana screenshot without dashboard changes**

Open the existing shared `DB Lab Overview` during or right after the sampling runs. Capture:

```text
Run Summary
k6 Load
Hikari Pool
Table Access for point_history
```

Save screenshot to:

```text
docs/evidence/phase-07/grafana/retest-db-lab-overview.png
```

Do not add Phase 7 specific panels or a new dashboard.

## Done When

- [ ] Offset k6 summary is saved.
- [ ] Cursor k6 summary is saved.
- [ ] Offset and Cursor `pg_stat_statements` snapshots are separated by reset.
- [ ] Cursor snapshot has no `count(*) from point_history` query.
- [ ] Existing `DB Lab Overview` screenshot is saved as auxiliary evidence.

