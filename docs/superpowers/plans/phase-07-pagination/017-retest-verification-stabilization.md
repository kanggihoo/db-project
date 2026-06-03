# 017 Retest Verification Stabilization

## Goal

Phase 7 재측정 변경이 코드, k6, SQL, 문서 측면에서 일관된지 최종 검증한다.

## Files

- Verify: `ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java`
- Verify: `scripts/phase-07/*.sql`
- Verify: `k6/points-offset-sampling-test.js`
- Verify: `k6/points-cursor-sampling-test.js`
- Verify: `docs/phases/07-pagination/report.md`
- Verify: `docs/evidence/phase-07/README.md`

## Steps

- [ ] **Step 1: Run Java tests**

Run from `ecommerce/`:

```bash
rtk ./gradlew test --tests "*PointHistoryRepositoryTest" --tests "*PointCursorApiTest"
```

Expected: selected tests pass.

- [ ] **Step 2: Compile application**

Run from `ecommerce/`:

```bash
rtk ./gradlew compileJava
```

Expected: compile succeeds.

- [ ] **Step 3: Check retest plan and docs references**

Run from repository root:

```bash
rtk rg -n "retest|points-offset-sampling|points-cursor-sampling|707000|DB Lab Overview|pg_stat_statements_reset|created_at DESC, id DESC" docs scripts k6 ecommerce/src/main ecommerce/src/test
```

Expected: references appear in the retest spec, retest plan docs, scripts, k6 files, and Phase 7 report/evidence docs.

- [ ] **Step 4: Check required files exist**

Run from repository root:

```bash
rtk proxy powershell -NoProfile -Command "$paths = @('docs/superpowers/plans/phase-07-pagination/010-retest-plan-index.md','scripts/phase-07/31-retest-offset-sampling-explain.sql','scripts/phase-07/32-retest-cursor-samples.sql','scripts/phase-07/33-retest-cursor-next-slice-explain.sql','scripts/phase-07/34-retest-count-only-explain.sql','k6/points-offset-sampling-test.js','k6/points-cursor-sampling-test.js','k6/presets/points-offset-sampling.json','k6/presets/points-cursor-sampling.json'); $paths | ForEach-Object { if (-not (Test-Path $_)) { throw \"Missing $_\" } }"
```

Expected: command exits successfully.

- [ ] **Step 5: Check report wording**

Run:

```bash
rtk rg -n "Page와 Cursor는 UX 목적이 다르다|Cursor source lookup|warm-cache|Grafana screenshot은 기존 shared DB Lab Overview" docs/phases/07-pagination/report.md
```

Expected: report includes all required interpretation anchors.

- [ ] **Step 6: Review git diff**

Run:

```bash
rtk git diff --stat
rtk git status --short
```

Expected: only Phase 7 retest code, SQL, k6, docs, and evidence files are changed.

## Done When

- [ ] Java tests pass.
- [ ] Application compiles.
- [ ] SQL and k6 retest files exist.
- [ ] Evidence index links all retest artifacts.
- [ ] Report describes Page/Cursor trade-off instead of a simple speed contest.
