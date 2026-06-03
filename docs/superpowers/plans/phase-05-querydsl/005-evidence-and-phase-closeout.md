# 005 Evidence And Phase Closeout

## Goal

Finalize Phase 5 documentation so the roadmap, phase docs, evidence index, and report agree with the implemented behavior.

## Files

- Modify: `docs/roadmap/06-phase-5-querydsl.md`
- Modify: `docs/phases/05-querydsl/README.md`
- Modify: `docs/phases/05-querydsl/scope.md`
- Modify: `docs/phases/05-querydsl/runbook.md`
- Modify: `docs/phases/05-querydsl/observability.md`
- Modify: `docs/phases/05-querydsl/report.md`
- Modify: `docs/evidence/phase-05/README.md`

## Steps

- [ ] **Step 1: Update roadmap to match the chosen strategy design**

Modify `docs/roadmap/06-phase-5-querydsl.md` so the Product search section states:

```markdown
Phase 5 exposes `strategy=baseline|querydsl` on the existing product search API for learning evidence. `baseline` keeps the Spring Data JPA entity query and `ProductResponse.from(product)` mapping. `querydsl` uses QueryDSL DTO projection and is the default when strategy is omitted.
```

Also ensure the monitoring/evidence section states:

```markdown
Phase 5 required evidence is integration test output, representative SQL, SQL count, and short summaries under `docs/evidence/phase-05/`. k6/Grafana and `pg_stat_statements` are optional, not required.
```

- [ ] **Step 2: Update Phase 5 README status**

Modify `docs/phases/05-querydsl/README.md`:

```markdown
## 현재 상태

Phase 5는 완료됐다.

- 기존 상품 검색 API에서 `strategy=baseline|querydsl` 비교를 지원한다.
- strategy 생략 시 QueryDSL 경로를 기본값으로 사용한다.
- product search evidence는 SQL shape와 테스트 출력 중심으로 기록했다.
- bulk update evidence는 SQL count와 persistence context clear behavior 중심으로 기록했다.
- k6/Grafana는 Phase 5 필수 evidence로 사용하지 않았다.
```

- [ ] **Step 3: Mark scope completion criteria**

Modify `docs/phases/05-querydsl/scope.md` completion checklist so completed items use `[x]` only after the corresponding code and evidence files exist:

```markdown
## 완료 조건

- [x] `GET /api/products`가 `strategy=baseline|querydsl`을 받는다.
- [x] strategy 생략 시 QueryDSL 경로를 사용한다.
- [x] baseline과 querydsl이 같은 공유 조건에서 같은 `ProductResponse` 값을 반환한다.
- [x] QueryDSL 경로가 null `categoryId`와 null `status` 조건을 predicate에서 제외한다.
- [x] row-by-row update와 bulk update의 SQL count 차이를 테스트 또는 evidence로 남긴다.
- [x] evidence가 `docs/evidence/phase-05/` 아래에 정리된다.
- [x] Phase 6 집계 쿼리 handoff가 report에 기록된다.
```

- [ ] **Step 4: Update report with measured results**

Modify `docs/phases/05-querydsl/report.md` with the final comparison:

```markdown
## 결론

Phase 5는 기존 Product entity 조회 baseline과 QueryDSL DTO projection을 같은 상품 검색 API에서 비교했다. 두 전략은 공유 조건에서 같은 `ProductResponse` 값을 반환했고, QueryDSL 경로는 필요한 응답 필드만 select하는 SQL shape를 만들었다.

k6/Grafana는 사용하지 않았다. Phase 5의 핵심 evidence는 representative SQL, Hibernate statistics SQL count, integration test output이다.

## Product Search 비교

| Strategy | Result | Evidence |
|---|---|---|
| baseline | Product entity columns 조회 후 `ProductResponse` 변환 | `docs/evidence/phase-05/product-search/baseline-sql.txt` |
| querydsl | `ProductResponse` 필드만 DTO projection | `docs/evidence/phase-05/product-search/querydsl-sql.txt` |

## Bulk Update 비교

| Strategy | Result | Evidence |
|---|---|---|
| row-by-row | 1 select + 3 update statements | `docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt` |
| bulk update | 1 update statement | `docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt` |

## Phase 6 Handoff

Phase 5는 단순 조회의 SQL shape와 update round-trip 차이를 정리했다. Phase 6에서는 Product/Review 집계 쿼리에서 GROUP BY, HAVING, expression index, aggregate execution plan을 비교한다.
```

- [ ] **Step 5: Verify all expected evidence files exist**

Run:

```bash
rtk proxy powershell -NoProfile -Command @'
$paths = @(
  'docs/evidence/phase-05/README.md',
  'docs/evidence/phase-05/product-search/measurement-condition.md',
  'docs/evidence/phase-05/product-search/baseline-sql.txt',
  'docs/evidence/phase-05/product-search/querydsl-sql.txt',
  'docs/evidence/phase-05/product-search/strategy-test-output.txt',
  'docs/evidence/phase-05/product-search/summary.md',
  'docs/evidence/phase-05/bulk-update/measurement-condition.md',
  'docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt',
  'docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt',
  'docs/evidence/phase-05/bulk-update/persistence-context-test-output.txt',
  'docs/evidence/phase-05/bulk-update/summary.md'
)
$missing = $paths | Where-Object { -not (Test-Path $_) }
if ($missing) {
  $missing
  exit 1
}
'@
```

Expected: command exits 0 with no missing path output.

- [ ] **Step 6: Verify markdown references to Phase 5**

Run:

```bash
rtk rg -n "Phase 5|phase-05|05-querydsl|QueryDSL|strategy" docs/roadmap/06-phase-5-querydsl.md docs/phases/05-querydsl docs/evidence/phase-05
```

Expected: output shows Phase 5 roadmap, phase docs, and evidence references.

- [ ] **Step 7: Commit**

```bash
git add docs/roadmap/06-phase-5-querydsl.md docs/phases/05-querydsl docs/evidence/phase-05
git commit -m "docs(phase5): finalize querydsl evidence report"
```
