# 005 Verification And Documentation

## Goal

k6 shared lib 리팩토링 후 실행 계약, label 계약, 문서 추적성을 검증하고 필요한 문서를 갱신한다.

## Files

- Modify if needed: `docs/guides/k6-load-testing.md`
- Modify if needed: `scripts/verify-observability.mjs`
- Modify if needed: `docs/superpowers/specs/2026-06-02-k6-lib-refactor-design.md`

## Steps

- [ ] **Step 1: Verify observability script still matches k6 files**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

If this fails because the verifier assumes old file structure or misses pagination scenarios, update `scripts/verify-observability.mjs` narrowly.

- [ ] **Step 2: Verify label contract remains visible**

Run:

```bash
rtk rg -n "phase:|scenario:|preset:|pool:|PRESET_NAME|expected_response" k6
```

Expected:

- common labels are still present through `k6/lib/config.js`
- system tags still include `expected_response`
- no high-cardinality request values are introduced as labels

- [ ] **Step 3: Verify no Node-only API in k6 lib**

Run:

```bash
rtk rg -n "node:|require\\(|process\\.|fs\\.|path\\.|npm" k6/lib
```

Expected: no Node-only API usage.

- [ ] **Step 4: Update k6 guide only if needed**

If `docs/guides/k6-load-testing.md` describes k6 script internals, update it to mention that scenario entrypoints now use shared helpers in `k6/lib/`.

Do not rewrite phase runbooks unless the command contract changed. The command contract should not change in this refactor.

- [ ] **Step 5: Check spec and plan consistency**

Run:

```bash
rtk rg -n "config.js|scenarios.js|checks.js|review-summary-test.js|tracer bullet|PRESET_NAME" docs/superpowers/specs/2026-06-02-k6-lib-refactor-design.md docs/superpowers/plans/k6-lib-refactor
```

Expected: spec and plans describe the same lib scope.

- [ ] **Step 6: Runtime verification when environment is available**

Run:

```bash
rtk npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --condition refactor-smoke
```

Optionally run:

```bash
rtk npm run k6:evidence -- --phase phase-07 --scenario points --preset points-page0 --condition refactor-smoke
```

If local services are not available, record that runtime verification was not run and keep static verification results.

- [ ] **Step 7: Commit**

```bash
git add docs/guides/k6-load-testing.md scripts/verify-observability.mjs docs/superpowers/specs/2026-06-02-k6-lib-refactor-design.md docs/superpowers/plans/k6-lib-refactor
git commit -m "docs(k6): document shared lib refactor plan"
```
