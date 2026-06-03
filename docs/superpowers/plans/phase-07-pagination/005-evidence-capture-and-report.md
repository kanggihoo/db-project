# 005 Evidence Capture And Report

## Goal

Phase 7 evidence를 수집하고 `docs/evidence/phase-07/README.md`, `docs/phases/07-pagination/report.md`에 결과를 연결한다.

## Files

- Modify: `docs/evidence/phase-07/README.md`
- Modify: `docs/phases/07-pagination/report.md`
- Create evidence files under `docs/evidence/phase-07/`

## Steps

- [ ] **Step 1: Create evidence directories**

Run:

```bash
rtk powershell -NoProfile -Command "New-Item -ItemType Directory -Force docs/evidence/phase-07/data-profile, docs/evidence/phase-07/explain, docs/evidence/phase-07/k6, docs/evidence/phase-07/pg-stat-statements, docs/evidence/phase-07/grafana | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 2: Capture data profile**

Run:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/00-data-profile.sql > docs/evidence/phase-07/data-profile/hot-user-point-counts.txt
```

Then create `docs/evidence/phase-07/data-profile/selected-user-and-pages.txt` with the selected values. The initial example below assumes `user_id=1`, `point_count=1800`, `size=20`, `maxPage=89`, `midPage=44`, and `deepPage=71`; replace the numbers with the values from `hot-user-point-counts.txt` before running k6:

```text
PHASE7_SELECTED_USER_AND_PAGES
user_id=1
point_count=1800
size=20
maxPage=89
midPage=44
deepPage=71
```

- [ ] **Step 3: Capture global SQL-only evidence**

Run:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/10-global-index-prepare.sql
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/11-global-offset-page0-explain.sql > docs/evidence/phase-07/explain/global-offset-page0.txt
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/12-global-offset-deep-explain.sql > docs/evidence/phase-07/explain/global-offset-deep.txt
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/13-global-cursor-deep-explain.sql > docs/evidence/phase-07/explain/global-cursor-deep.txt
```

Expected: each explain file contains `Execution Time` and `Buffers`.

- [ ] **Step 4: Capture hot user SQL evidence**

The command below uses conservative defaults from the example profile. Replace `$env:PHASE7_USER_ID` and `$env:PHASE7_DEEP_OFFSET` with values from `selected-user-and-pages.txt` when the selected user differs.

Run:

```bash
$env:PHASE7_USER_ID='1'
$env:PHASE7_DEEP_OFFSET='1420'
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/20-user-index-prepare.sql
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=$env:PHASE7_USER_ID -v deep_offset=$env:PHASE7_DEEP_OFFSET -f /workspace/scripts/phase-07/21-user-offset-page0-explain.sql > docs/evidence/phase-07/explain/user-offset-page0.txt
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=$env:PHASE7_USER_ID -v deep_offset=$env:PHASE7_DEEP_OFFSET -f /workspace/scripts/phase-07/22-user-offset-deep-explain.sql > docs/evidence/phase-07/explain/user-offset-deep.txt
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=$env:PHASE7_USER_ID -v deep_offset=$env:PHASE7_DEEP_OFFSET -f /workspace/scripts/phase-07/23-user-cursor-deep-explain.sql > docs/evidence/phase-07/explain/user-cursor-deep.txt
```

- [ ] **Step 5: Capture Offset k6 summaries**

Run each k6 condition and save stdout.

```bash
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=page0 PRESET=presets/points-page0.json k6 run k6/points-test.js > docs/evidence/phase-07/k6/offset-page0-summary.txt
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=mid PRESET=presets/points-mid.json k6 run k6/points-test.js > docs/evidence/phase-07/k6/offset-mid-summary.txt
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=deep PRESET=presets/points-deep.json k6 run k6/points-test.js > docs/evidence/phase-07/k6/offset-deep-summary.txt
```

- [ ] **Step 6: Capture Offset pg_stat_statements**

Run immediately after Offset deep k6:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/30-pg-stat-statements.sql > docs/evidence/phase-07/pg-stat-statements/offset-page-api.txt
```

- [ ] **Step 7: Capture Cursor k6 summary**

Run:

```bash
PHASE=phase-07 SCENARIO=points-cursor PRESET_NAME=cursor PRESET=presets/points-cursor.json k6 run k6/points-cursor-test.js > docs/evidence/phase-07/k6/cursor-summary.txt
```

- [ ] **Step 8: Capture Cursor pg_stat_statements**

Run immediately after Cursor k6:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/30-pg-stat-statements.sql > docs/evidence/phase-07/pg-stat-statements/cursor-api.txt
```

- [ ] **Step 9: Save Grafana screenshot**

Capture shared `DB Lab Overview` during or immediately after the runs and save:

```text
docs/evidence/phase-07/grafana/db-lab-overview-phase-7.png
```

Capture Run Summary, k6 Load, Hikari Pool, and Table Access with `$table` including `point_history`.

- [ ] **Step 10: Update evidence index with measured values**

Modify `docs/evidence/phase-07/README.md`:

```md
## Result Summary

| 비교 | 결과 |
|---|---|
| global offset deep vs cursor deep | `global-offset-deep.txt`와 `global-cursor-deep.txt`의 `Execution Time` 비교값을 기록 |
| user offset page0 vs deep | `user-offset-page0.txt`와 `user-offset-deep.txt`의 `Execution Time` 비교값을 기록 |
| offset deep vs cursor API p95 | `offset-deep-summary.txt`와 `cursor-summary.txt`의 p95 비교값을 기록 |
| Offset/Page count query | `offset-page-api.txt`의 `count(*) from point_history` 호출 수와 평균 실행시간 기록 |
| Cursor count query | `cursor-api.txt`에서 count query 부재 또는 호출 수 기록 |
```

- [ ] **Step 11: Update report**

Modify `docs/phases/07-pagination/report.md` so every placeholder table row links to the captured evidence files and records observed p95 or execution time.

- [ ] **Step 12: Verify evidence references**

Run:

```bash
rtk rg -n "global-offset-page0|global-offset-deep|global-cursor-deep|offset-page0-summary|cursor-summary|offset-page-api|cursor-api|db-lab-overview-phase-7" docs/evidence/phase-07 docs/phases/07-pagination/report.md
```

Expected: all evidence names are referenced.

- [ ] **Step 13: Commit**

```bash
git add docs/evidence/phase-07 docs/phases/07-pagination/report.md
git commit -m "docs(phase7): capture pagination evidence"
```
