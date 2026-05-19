# 009 Report And Phase Closeout

## Goal

Write the final Phase 3 report after evidence collection and record the trade-offs that move the project to Phase 4.

## Files

- Modify: `docs/phases/03-n-plus-one/README.md`
- Modify: `docs/phases/03-n-plus-one/report.md`
- Modify: `docs/evidence/phase-03/README.md`

## Steps

- [ ] **Step 1: Verify evidence files exist**

Run:

```bash
rtk proxy find docs/evidence/phase-03/orders -maxdepth 2 -type f | sort
```

Expected: each completed strategy has `k6-summary.txt`, `pg-stat-statements.txt`, `sql-count.txt`, `representative-sql.txt`, `explain.txt`, and `grafana-screenshot.png`.

- [ ] **Step 2: Extract strategy summary values**

For each strategy, read:

```bash
rtk read docs/evidence/phase-03/orders/lazy/sql-count.txt
rtk read docs/evidence/phase-03/orders/lazy/k6-summary.txt
rtk read docs/evidence/phase-03/orders/lazy/pg-stat-statements.txt
```

Repeat for `fetch-join`, `batch-size`, and `entity-graph`. Record:

- request-level SQL count
- k6 p95
- failure rate
- dropped iterations
- top query calls and total time
- Hikari pending observation from Grafana screenshot or notes

- [ ] **Step 3: Update `report.md` conclusion**

Replace the initial report with measured sections:

```markdown
# Phase 3 결과 보고서

## 결론

Phase 3는 주문 목록 썸네일 조회에서 Lazy naive가 요청당 SQL 수를 크게 늘리는 것을 확인했다. Fetch Join은 주요 단일 연관 경로의 반복 select를 줄였고, BatchSize는 Lazy 접근을 유지하면서 `IN (...)` 쿼리로 반복 조회를 완화했다.

## 측정 조건

| 항목 | 값 |
|---|---|
| phase | `phase-03` |
| scenario | `orders` |
| preset | `baseline` |
| pool | `pool10` |
| API | `GET /api/orders?userId=&strategy=` |
| entity path | `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` |
```

- [ ] **Step 4: Add the measured strategy comparison table**

Add a `## 전략별 비교` section to `docs/phases/03-n-plus-one/report.md`.

The table must use this exact row order and fill every metric from Step 2 evidence before saving the report:

- `lazy`: SQL count/request, k6 p95, failed rate, dropped iterations, Hikari pending, main SQL shape `repeated select`
- `fetch-join`: SQL count/request, k6 p95, failed rate, dropped iterations, Hikari pending, main SQL shape `join fetch`
- `batch-size`: SQL count/request, k6 p95, failed rate, dropped iterations, Hikari pending, main SQL shape `IN (...)`
- `entity-graph`: SQL count/request, k6 p95, failed rate, dropped iterations, Hikari pending, main SQL shape `graph loading`

- [ ] **Step 5: Add interpretation sections**

Add:

```markdown
## 해석

### Lazy naive

Lazy naive는 기본 `LAZY` 매핑 자체가 문제가 아니라, 주문 목록 응답 조립 과정에서 연관 객체를 순차 접근하면서 요청당 SQL 수가 증가한다는 점을 보여준다.

### Fetch Join

Fetch Join은 `Orders -> OrderItems -> ProductSku -> Product` 경로를 한 번에 당겨 반복 select를 줄인다. 다만 collection fetch join은 row duplication을 만들 수 있고, 페이징과 함께 사용할 때 Hibernate 경고와 메모리 페이징 위험이 있다.

### BatchSize

BatchSize는 Lazy 접근을 유지하면서 개별 select를 `IN (...)` 쿼리로 묶는다. 쿼리 수를 1회로 만들지는 않지만 주문 목록처럼 페이징과 다중 연관이 섞이는 조회에서 더 안전한 선택지가 될 수 있다.

### EntityGraph

EntityGraph는 repository method에 조회 시점 로딩 범위를 선언한다. Fetch Join과 유사한 효과를 낼 수 있지만, 복잡한 조건/정렬/페이징이 필요해질수록 JPQL 또는 QueryDSL 쪽이 더 명확할 수 있다.

## Phase 4 Handoff

Phase 3는 조회 로딩 전략 문제를 다뤘다. 다음 질문은 조회가 아니라 동시에 여러 사용자가 주문/재고를 변경할 때 데이터 정합성이 어떻게 유지되는가다. Phase 4는 트랜잭션 격리 수준과 재고 정합성 실험으로 넘어간다.
```

- [ ] **Step 6: Update Phase README status**

In `docs/phases/03-n-plus-one/README.md`, change status to completed:

```markdown
## 현재 상태

Phase 3는 완료된 상태다.

- Lazy naive N+1 재현 완료
- Fetch Join, BatchSize, EntityGraph 전략 비교 완료
- SQL count, `pg_stat_statements`, k6, Grafana, EXPLAIN evidence 저장 완료
- Phase 4 transaction isolation handoff 기록 완료
```

- [ ] **Step 7: Update evidence README with final links**

Ensure `docs/evidence/phase-03/README.md` links each strategy file using markdown links, for example:

```markdown
| Lazy k6 summary | [orders/lazy/k6-summary.txt](./orders/lazy/k6-summary.txt) |
| Lazy pg_stat_statements | [orders/lazy/pg-stat-statements.txt](./orders/lazy/pg-stat-statements.txt) |
```

- [ ] **Step 8: Verify report links and measured values**

Run:

```bash
rtk grep "측정 전" docs/phases/03-n-plus-one/report.md
rtk read docs/phases/03-n-plus-one/report.md
rtk read docs/evidence/phase-03/README.md
```

Expected:

- `rtk grep "측정 전"` returns no matches.
- report and evidence README are readable.

- [ ] **Step 9: Commit**

```bash
git add docs/phases/03-n-plus-one docs/evidence/phase-03/README.md
git commit -m "docs: close out phase 3 n plus one results"
```
