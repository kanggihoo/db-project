# 005 Report And Phase Closeout

## Goal

Turn raw Phase 2 evidence into a final report and state whether the next bottleneck should move to Phase 3.

## Files

- Modify: `docs/phases/02-indexes/report.md`
- Modify: `docs/phases/02-indexes/README.md`
- Modify: `docs/evidence/phase-02/README.md`

## Steps

- [ ] **Step 1: Extract Phase 2 post-index k6 values**

Read:

```bash
rtk read docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
```

Record these values:

- requests
- `http_req_duration` p95
- `http_req_failed`
- `dropped_iterations`

Expected: the file contains a completed k6 summary.

- [ ] **Step 2: Extract Phase 2 post-index SQL values**

Read:

```bash
rtk read docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

Record these values for the product query:

- calls
- mean_ms
- total_ms
- rows

Expected: the file contains a product query row from `pg_stat_statements`.

- [ ] **Step 3: Extract main EXPLAIN plan names**

Run:

```bash
rtk grep "Scan" docs/evidence/phase-02/products/pre-index/explain.txt
rtk grep "Scan" docs/evidence/phase-02/products/pool10-post-index/explain.txt
```

Expected: pre-index and post-index scan nodes are visible. Record the actual scan type rather than assuming a specific planner choice.

- [ ] **Step 4: Update report conclusion**

In `docs/phases/02-indexes/report.md`, replace the initial conclusion with a concrete paragraph using the scan names found in Step 3. The paragraph must state:

- Phase 2 kept the existing product API flow.
- The main index was `idx_product_category_status`.
- The pre-index scan type from Step 3.
- The post-index scan type from Step 3.
- The comparison table below contains the measured API and SQL values.

- [ ] **Step 5: Add comparison table**

Add a `## Phase 1 대비 Phase 2 비교` table to `docs/phases/02-indexes/report.md`.

Use these columns:

- `Metric`
- `Phase 1 Baseline`
- `Phase 2 Post-index`

Use these rows and Phase 1 values:

| Metric | Phase 1 Baseline | Phase 2 source |
|---|---:|---|
| API p95 | 17.24ms | Step 1 k6 p95 |
| SQL mean time | 7.90ms | Step 2 `mean_ms` |
| SQL total time | 118,489.64ms | Step 2 `total_ms` |
| requests | 15,001 | Step 1 request count |
| failed | 0.00% | Step 1 failed rate |
| dropped iterations | 0 | Step 1 dropped iteration count |

The final report must contain the exact measured Phase 2 values, not source-file descriptions.

- [ ] **Step 6: Add evidence links**

Add this section:

```markdown
## Evidence

- [pre-index EXPLAIN](../../evidence/phase-02/products/pre-index/explain.txt)
- [post-index EXPLAIN](../../evidence/phase-02/products/pool10-post-index/explain.txt)
- [post-index k6 summary](../../evidence/phase-02/products/pool10-post-index/k6-summary.txt)
- [post-index pg_stat_statements](../../evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt)
- [Grafana screenshot](../../evidence/phase-02/grafana-screenshots/products-post-index.png)
- [SQL-only evidence index](../../evidence/phase-02/README.md)
```

- [ ] **Step 7: Add Phase 3 handoff**

Add this section:

```markdown
## Phase 3 Handoff

Phase 2는 상품 검색 쿼리의 인덱스 설계와 실행계획 분석을 다뤘다. 주문 목록 조회의 반복적인 `order_item where order_id = ?` 호출은 Phase 1에서 이미 별도 병목으로 확인됐고, 인덱스 실험의 직접 해결 대상이 아니다.

따라서 다음 병목은 쿼리 수 문제로 분류하고 Phase 3 N+1 / 로딩 전략 최적화에서 다룬다.
```

- [ ] **Step 8: Update Phase README status**

In `docs/phases/02-indexes/README.md`, change:

```markdown
Phase 2는 준비 중이다.
```

to:

```markdown
Phase 2는 완료된 상태다.
```

Then add bullets under current status:

```markdown
- 메인 상품 검색 쿼리의 pre-index/post-index 실행계획 기록 완료
- `idx_product_category_status` 적용 후 k6, `pg_stat_statements`, Grafana evidence 저장 완료
- SQL-only 보조 실험으로 단일/복합/커버링/부분 인덱스 동작 확인 완료
- Phase 3 N+1 최적화로 넘길 근거 기록 완료
```

- [ ] **Step 9: Verify report has no measurement placeholders**

Run:

```bash
rtk grep "measured from\\|<actual\\|측정 전이다" docs/phases/02-indexes/report.md
```

Expected: zero matches. If matches remain, replace them with measured values or concrete wording from the evidence.

- [ ] **Step 10: Commit**

```bash
git add docs/phases/02-indexes/README.md docs/phases/02-indexes/report.md docs/evidence/phase-02/README.md
git commit -m "docs: finalize phase 2 index report"
```
