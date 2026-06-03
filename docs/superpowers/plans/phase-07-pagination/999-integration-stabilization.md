# 999 Integration Stabilization

## Goal

Phase 7 구현, 테스트, 문서, evidence 구조를 최종 검증한다.

## Files

- Verify: `docs/roadmap/08-phase-7-pagination.md`
- Verify: `docs/superpowers/specs/2026-05-27-phase-7-pagination-design.md`
- Verify: `docs/phases/07-pagination/*`
- Verify: `docs/evidence/phase-07/*`
- Verify: `scripts/phase-07/*`
- Verify: `ecommerce/src/main/java/com/dblab/ecommerce/**`
- Verify: `ecommerce/src/test/java/com/dblab/ecommerce/**`
- Verify: `k6/*`

## Steps

- [ ] **Step 1: Run focused Java tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*PointHistoryRepositoryTest" --tests "*PointCursorApiTest"
```

Expected: Gradle exits 0.

- [ ] **Step 2: Run compile check**

Run:

```bash
cd ecommerce && rtk gradlew compileJava
```

Expected: Gradle exits 0.

- [ ] **Step 3: Run observability verification**

Run:

```bash
rtk node scripts/verify-observability.mjs
```

Expected: Node exits 0.

- [ ] **Step 4: Verify required Phase 7 files exist**

Run:

```bash
rtk powershell -NoProfile -Command "$paths = @('docs/phases/07-pagination/README.md','docs/phases/07-pagination/scope.md','docs/phases/07-pagination/runbook.md','docs/phases/07-pagination/observability.md','docs/phases/07-pagination/report.md','docs/evidence/phase-07/README.md','scripts/phase-07/00-data-profile.sql','scripts/phase-07/10-global-index-prepare.sql','scripts/phase-07/20-user-index-prepare.sql','k6/points-cursor-test.js','k6/presets/points-cursor.json'); $paths | ForEach-Object { if (-not (Test-Path $_)) { throw \"Missing $_\" } }"
```

Expected: command exits 0.

- [ ] **Step 5: Verify docs do not contradict scope**

Run:

```bash
rtk rg -n "Delivery Tracking|seed-phase7|page=1000|O\\(1\\)|rows removed by filter|/api/points/cursor|idx_point_history_user_created_id" docs/roadmap/08-phase-7-pagination.md docs/superpowers/specs/2026-05-27-phase-7-pagination-design.md docs/phases/07-pagination docs/evidence/phase-07/README.md
```

Expected:

- `Delivery Tracking` appears only as excluded or non-required candidate.
- `page=1000`, `O(1)`, and `rows removed by filter` do not appear as Phase 7 claims.
- `/api/points/cursor` and `idx_point_history_user_created_id` appear in implementation docs.

- [ ] **Step 6: Verify evidence files contain expected markers after capture**

Run after evidence capture:

```bash
rtk rg -n "PHASE7_|Execution Time|Buffers|http_req_duration|point_history|count\\(\\*\\)|select count" docs/evidence/phase-07
```

Expected: markers are present in relevant evidence files.

- [ ] **Step 7: Review git diff**

Run:

```bash
rtk git diff --stat
rtk git diff -- docs/roadmap/08-phase-7-pagination.md docs/superpowers/specs/2026-05-27-phase-7-pagination-design.md docs/superpowers/plans/phase-07-pagination
```

Expected: changes are scoped to Phase 7 docs/plans and the implementation files from the completed slices.

- [ ] **Step 8: Final commit**

```bash
git add docs/phases/07-pagination docs/evidence/phase-07 docs/superpowers/plans/phase-07-pagination scripts/phase-07 k6 ecommerce/src/main ecommerce/src/test docs/guides scripts/verify-observability.mjs
git commit -m "chore(phase7): stabilize pagination phase"
```

