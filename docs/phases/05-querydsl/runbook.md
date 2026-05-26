# Phase 5 Runbook

별도 언급이 없으면 명령은 `ecommerce/`에서 실행한다.

## Focused Test 명령

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest"
rtk gradlew test --tests "*OrderBulkUpdateTest"
rtk gradlew compileJava
```

## Evidence Capture 명령

Focused test 출력과 대표 SQL snapshot을 `docs/evidence/phase-05/` 아래에 저장한다.

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest" --info > ../docs/evidence/phase-05/product-search/strategy-test-output.txt
rtk gradlew test --tests "*OrderBulkUpdateTest" --info > ../docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt
```

SQL snapshot을 갱신해야 한다면 focused test 실행 시 Hibernate SQL logging을 켜고 대표 SQL shape만 저장한다.

## 예상 Evidence 파일

- `docs/evidence/phase-05/README.md`
- `docs/evidence/phase-05/product-search/measurement-condition.md`
- `docs/evidence/phase-05/product-search/baseline-sql.txt`
- `docs/evidence/phase-05/product-search/querydsl-sql.txt`
- `docs/evidence/phase-05/product-search/strategy-test-output.txt`
- `docs/evidence/phase-05/product-search/summary.md`
- `docs/evidence/phase-05/bulk-update/measurement-condition.md`
- `docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt`
- `docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt`
- `docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt`
- `docs/evidence/phase-05/bulk-update/summary.md`

## Closeout 확인

repository root에서 실행한다.

```powershell
rtk powershell -NoProfile -Command "$paths = @('docs/evidence/phase-05/README.md','docs/evidence/phase-05/product-search/measurement-condition.md','docs/evidence/phase-05/product-search/baseline-sql.txt','docs/evidence/phase-05/product-search/querydsl-sql.txt','docs/evidence/phase-05/product-search/strategy-test-output.txt','docs/evidence/phase-05/product-search/summary.md','docs/evidence/phase-05/bulk-update/measurement-condition.md','docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt','docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt','docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt','docs/evidence/phase-05/bulk-update/summary.md'); $paths | ForEach-Object { [pscustomobject]@{ Path = $_; Exists = Test-Path $_ } }"
rtk rg -n "Phase 5|phase-05|05-querydsl|QueryDSL|strategy" docs/roadmap/06-phase-5-querydsl.md docs/phases/05-querydsl docs/evidence/phase-05
```
