# 001 Phase Documentation Scaffold

## Goal

Create the Phase 3 documentation and evidence directory scaffold before changing application behavior.

## Files

- Create: `docs/phases/03-n-plus-one/README.md`
- Create: `docs/phases/03-n-plus-one/scope.md`
- Create: `docs/phases/03-n-plus-one/runbook.md`
- Create: `docs/phases/03-n-plus-one/observability.md`
- Create: `docs/phases/03-n-plus-one/report.md`
- Create: `docs/evidence/phase-03/README.md`
- Create directories under `docs/evidence/phase-03/orders/`
- Modify: `docs/superpowers/README.md`

## Steps

- [ ] **Step 1: Create Phase 3 directories**

Run:

```bash
rtk proxy mkdir -p \
  docs/phases/03-n-plus-one \
  docs/evidence/phase-03/orders/lazy \
  docs/evidence/phase-03/orders/fetch-join \
  docs/evidence/phase-03/orders/batch-size \
  docs/evidence/phase-03/orders/entity-graph \
  docs/evidence/phase-03/grafana-screenshots
```

Expected: command exits 0.

- [ ] **Step 2: Create `docs/phases/03-n-plus-one/README.md`**

Use this content:

```markdown
# Phase 3. N+1 + 로딩 전략 최적화

Phase 3는 주문 목록 화면의 상품 썸네일 요구사항을 기준으로 `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` 조회 경로에서 N+1이 어떻게 발생하고, JPA 로딩 전략에 따라 SQL shape와 부하 지표가 어떻게 달라지는지 확인하는 Learning Phase다.

## 현재 상태

Phase 3는 준비 중이다.

- API는 `GET /api/orders?userId=&strategy=`를 사용한다.
- 비교 전략은 `lazy`, `fetch-join`, `batch-size`, `entity-graph`다.
- 핵심 evidence는 요청당 SQL 수, `pg_stat_statements`, k6, Grafana, 대표 EXPLAIN이다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 3 범위와 제외 범위 |
| [runbook.md](./runbook.md) | 반복 가능한 실행 절차 |
| [observability.md](./observability.md) | SQL count, k6, Grafana, EXPLAIN 해석 기준 |
| [report.md](./report.md) | 전략별 측정 결과와 Phase 4 handoff |

## Source Documents

- Roadmap: [docs/roadmap/04-phase-3-n-plus-one.md](../../roadmap/04-phase-3-n-plus-one.md)
- Spec: [docs/superpowers/specs/phase-3-n-plus-one-spec.md](../../superpowers/specs/phase-3-n-plus-one-spec.md)
- Plan: [docs/superpowers/plans/phase-03-n-plus-one/index.md](../../superpowers/plans/phase-03-n-plus-one/index.md)
- Phase 1 baseline: [docs/evidence/phase-01/BASELINE.md](../../evidence/phase-01/BASELINE.md)

## 관련 산출물

- Phase Evidence: [docs/evidence/phase-03/README.md](../../evidence/phase-03/README.md)
- 공통 k6 가이드: [docs/guides/k6-load-testing.md](../../guides/k6-load-testing.md)
- 공통 Grafana 가이드: [docs/guides/grafana-observability.md](../../guides/grafana-observability.md)

## 다음 Phase 연결

Phase 3 이후 조회 쿼리 수 문제가 정리되면, Phase 4에서는 동시에 여러 사용자가 주문/재고를 변경할 때의 데이터 정합성과 격리 수준 문제로 넘어간다.
```

- [ ] **Step 3: Create `docs/phases/03-n-plus-one/scope.md`**

Use this content:

```markdown
# Phase 3 범위

## 목표

Phase 3의 목표는 주문 목록 화면의 상품 썸네일 조회를 기준으로 Lazy naive, Fetch Join, BatchSize, EntityGraph 전략의 SQL count와 부하 지표 차이를 기록하는 것이다.

## 대상 API와 조회 경로

| 대상 | 값 |
|---|---|
| API | `GET /api/orders?userId=&strategy=` |
| Strategies | `lazy`, `fetch-join`, `batch-size`, `entity-graph` |
| Entity path | `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` |
| Snapshot fields | `OrderItem.productName`, `optionInfo`, `quantity`, `unitPrice` |
| Thumbnail source | `Product.images`에서 대표 이미지 |

## 제외 범위

- 기본 fetch type을 `EAGER`로 고정하지 않는다.
- `order_item.product_name`으로 `Product`를 역조회하지 않는다.
- QueryDSL DTO projection 최적화는 Phase 5로 남긴다.
- 동시성, 격리 수준, 재고 정합성은 Phase 4로 남긴다.
- 인덱스 실행계획 전환 자체를 Phase 3의 핵심 목표로 삼지 않는다.

## 완료 조건

- [ ] Lazy naive에서 `1 + N + 3M` 형태의 반복 쿼리가 재현됐다.
- [ ] Fetch Join과 BatchSize 전략의 SQL count, `pg_stat_statements`, k6 결과를 저장했다.
- [ ] EntityGraph 전략의 결과를 저장하거나 제외 사유를 report에 기록했다.
- [ ] 대표 SQL의 `EXPLAIN (ANALYZE, BUFFERS)`를 저장했다.
- [ ] Grafana Phase 3 evidence에서 latency, failure, dropped iterations, Hikari pressure를 비교했다.
- [ ] Phase 4로 넘길 동시성/정합성 질문을 report에 기록했다.
```

- [ ] **Step 4: Create `docs/evidence/phase-03/README.md`**

Use this content:

```markdown
# Phase 3 Evidence

Phase 3 evidence는 주문 목록 API의 N+1 재현과 로딩 전략별 개선 결과를 보관한다.

## Strategy Evidence

| Strategy | Path |
|---|---|
| Lazy naive | [orders/lazy/](./orders/lazy/) |
| Fetch Join | [orders/fetch-join/](./orders/fetch-join/) |
| BatchSize | [orders/batch-size/](./orders/batch-size/) |
| EntityGraph | [orders/entity-graph/](./orders/entity-graph/) |

## Expected Files Per Strategy

| File | Purpose |
|---|---|
| `k6-summary.txt` | k6 stdout/stderr summary |
| `pg-stat-statements.txt` | query shape별 calls, mean time, total time |
| `sql-count.txt` | 단일 요청 SQL count |
| `representative-sql.txt` | 전략별 대표 SQL 원문 |
| `explain.txt` | 대표 SQL의 `EXPLAIN (ANALYZE, BUFFERS)` |
| `grafana-screenshot.png` | Phase 3 focus screenshot |

## Notes

- 각 전략 측정 전 `pg_stat_statements_reset()`을 실행한다.
- k6 label은 `phase=phase-03`, `scenario=orders`, `preset=<preset>`, `pool=<pool>`, `strategy=<strategy>`를 사용한다.
- SQL 원문은 Prometheus label로 올리지 않고 evidence 파일로만 저장한다.
```

- [ ] **Step 5: Create initial runbook, observability, and report files**

Use these concise initial files. They will be expanded in later slices.

`docs/phases/03-n-plus-one/runbook.md`:

````markdown
# Phase 3 실행 절차

## 전체 흐름

```text
1. Phase 3 코드와 문서 준비
2. strategy별 단일 요청 SQL count 확인
3. strategy별 smoke k6 실행
4. strategy별 baseline k6 실행
5. pg_stat_statements, EXPLAIN, Grafana evidence 저장
6. report.md 작성
```

## 기본 실행 형식

```bash
PHASE=phase-03 STRATEGY=lazy ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=fetch-join ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=batch-size ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=entity-graph ./k6/run.sh orders smoke prometheus
```
````

`docs/phases/03-n-plus-one/observability.md`:

```markdown
# Phase 3 관측 전략

Phase 3의 핵심 지표는 요청당 SQL 수와 query shape별 calls다. Grafana는 latency, failure, dropped iterations, Hikari active/pending/acquire time, PostgreSQL active sessions를 확인하는 보조 evidence로 사용한다.

## 핵심 Evidence

| Evidence | Purpose |
|---|---|
| `sql-count.txt` | 단일 요청 SQL 수 확인 |
| `pg-stat-statements.txt` | 부하 중 query shape별 calls와 total time 확인 |
| `k6-summary.txt` | p95/p99, failure, dropped iterations 확인 |
| `grafana-screenshot.png` | 커넥션 점유와 latency 흐름 확인 |
| `explain.txt` | 대표 SQL 하나의 실행계획 확인 |
```

`docs/phases/03-n-plus-one/report.md`:

```markdown
# Phase 3 결과 보고서

## 결론

Phase 3 측정 전이다. 이 문서는 Lazy, Fetch Join, BatchSize, EntityGraph 전략 evidence 수집 후 업데이트한다.

## 전략별 비교

| Strategy | SQL count/request | k6 p95 | failed | dropped | Hikari pending | Notes |
|---|---:|---:|---:|---:|---:|---|
| lazy | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | N+1 baseline |
| fetch-join | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | join row duplication 확인 |
| batch-size | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | `IN (...)` query 확인 |
| entity-graph | 측정 전 | 측정 전 | 측정 전 | 측정 전 | 측정 전 | annotation 기반 graph |

## Phase 4 Handoff

조회 쿼리 수 최적화 후 남는 질문은 동시 주문/재고 변경 시 데이터 정합성과 격리 수준 문제다.
```

- [ ] **Step 6: Update `docs/superpowers/README.md`**

Add this line under current artifacts:

```markdown
- [Phase 3 N+1 Loading Strategy Plan](./plans/phase-03-n-plus-one/index.md)
```

- [ ] **Step 7: Verify scaffold**

Run:

```bash
rtk proxy find docs/phases/03-n-plus-one -maxdepth 1 -type f | sort
rtk read docs/evidence/phase-03/README.md
rtk grep "Phase 3 N+1 Loading Strategy Plan" docs/superpowers/README.md
```

Expected:

- Five standard Phase 3 files exist under `docs/phases/03-n-plus-one/`.
- `docs/evidence/phase-03/README.md` is readable.
- `docs/superpowers/README.md` links the Phase 3 plan index.

- [ ] **Step 8: Commit**

```bash
git add docs/phases/03-n-plus-one docs/evidence/phase-03/README.md docs/evidence/phase-03/orders docs/superpowers/README.md
git commit -m "docs: scaffold phase 3 n plus one documentation"
```
