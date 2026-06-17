# 001 Phase Documentation Scaffold

## Goal

Create the standard Phase 6 phase documentation and initial evidence index before code or evidence capture begins.

## Files

- Create: `docs/phases/06-aggregation/README.md`
- Create: `docs/phases/06-aggregation/scope.md`
- Create: `docs/phases/06-aggregation/runbook.md`
- Create: `docs/phases/06-aggregation/observability.md`
- Create: `docs/phases/06-aggregation/report.md`
- Create: `docs/evidence/phase-06/README.md`

## Steps

- [ ] **Step 1: Create Phase 6 documentation and evidence directories**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'docs/phases/06-aggregation','docs/evidence/phase-06' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 2: Add Phase 6 README**

Create `docs/phases/06-aggregation/README.md`:

```markdown
# Phase 6. 집계 쿼리 최적화

Phase 6은 PostgreSQL 집계 쿼리에서 baseline, naive index, query-shaped index condition이 scan, join, sort, aggregate plan을 어떻게 바꾸는지 측정하는 Learning Phase다.

## 현재 상태

Phase 6은 진행 전이다.

- primary evidence는 `psql` 기반 `EXPLAIN (ANALYZE, BUFFERS)`다.
- k6/Grafana는 Product Review Summary API의 representative evidence로만 사용한다.
- Phase 6 custom index는 `scripts/phase-06/*-prepare.sql`에서만 생성/삭제한다.
- `docker/postgres/init.sql`에는 Phase 6 실험 인덱스를 추가하지 않는다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 6 범위와 제외 범위 |
| [runbook.md](./runbook.md) | SQL-only와 k6 evidence capture 절차 |
| [observability.md](./observability.md) | 실행계획과 API 지표 해석 기준 |
| [report.md](./report.md) | 결과 보고서와 Phase 7 handoff |

## Source Documents

- Roadmap: [docs/roadmap/07-phase-6-aggregation.md](../../roadmap/07-phase-6-aggregation.md)
- Design Spec: [docs/superpowers/specs/2026-05-26-phase-6-aggregation-design.md](../../superpowers/specs/2026-05-26-phase-6-aggregation-design.md)
- Phase 5 handoff: [docs/phases/05-querydsl/report.md](../05-querydsl/report.md)

## 관련 산출물

- Phase Evidence: [docs/evidence/phase-06/README.md](../../evidence/phase-06/README.md)
```

- [ ] **Step 3: Add Phase 6 scope**

Create `docs/phases/06-aggregation/scope.md`:

```markdown
# Phase 6 Scope

## 목표

Product Review Summary와 Monthly Order Aggregate 집계 쿼리를 baseline, naive index, query-shaped index condition에서 비교한다.

## 포함 범위

| Area | Scope |
|---|---|
| SQL-only target 1 | Product Review Summary: `product` + `review` |
| SQL-only target 2 | Monthly Order Aggregate: `orders` + `users` |
| API target | `GET /api/products/review-summary` |
| Index conditions | baseline, naive index, query-shaped index |
| SQL evidence | `EXPLAIN (ANALYZE, BUFFERS)` |
| API evidence | review summary API k6 naive/query-shaped 비교 |
| Data profile | row count, review 분포, 월별 order 분포, index state |

## 제외 범위

- Phase 6 custom index를 `docker/postgres/init.sql`에 추가하지 않는다.
- Product Review Summary API에 request parameter를 추가하지 않는다.
- QueryDSL로 Product Review Summary API를 구현하지 않는다.
- 모든 SQL condition을 k6로 반복하지 않는다.
- pagination 최적화는 Phase 7로 남긴다.

## 완료 조건

- [ ] `product`, `review`, `orders`, `users` row count와 review/monthly order 분포를 evidence로 기록했다.
- [ ] Phase 6 SQL-only evidence를 재현할 수 있는 번호 기반 `scripts/phase-06/*.sql` 파일을 작성했다.
- [ ] condition별 `prepare` SQL에서 custom index 상태, `VACUUM (ANALYZE)`, `pg_stat_statements_reset()` 실행 조건을 분리했다.
- [ ] Product Review Summary 집계 쿼리를 baseline, naive index, query-shaped index condition에서 비교했다.
- [ ] Monthly Order Aggregate 쿼리를 baseline, naive index, query-shaped index condition에서 비교했다.
- [ ] 각 condition의 `EXPLAIN (ANALYZE, BUFFERS)`를 `docs/evidence/phase-06/` 아래에 저장했다.
- [ ] Product Review Summary API를 naive index condition과 query-shaped index condition에서 k6로 비교했다.
- [ ] `HashAggregate`, `GroupAggregate`, `Sort`, scan 방식 변화와 execution time 차이를 report에 해석했다.
- [ ] `DATE_TRUNC` expression index의 생성 가능 여부, planner 사용 여부, 실행시간 차이를 기록했다.
- [ ] Phase 7 pagination 병목과 Phase 6 집계 쿼리 병목을 분리해 handoff를 기록했다.
```

- [ ] **Step 4: Add Phase 6 runbook**

Create `docs/phases/06-aggregation/runbook.md`:

```markdown
# Phase 6 Runbook

별도 언급이 없으면 명령은 repository root에서 실행한다.

## Measurement database reset

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

## SQL-only evidence

Data profile:

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/00-data-profile.sql > docs/evidence/phase-06/data-profile/row-counts.txt
```

Product Review Summary:

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/10-review-baseline-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/11-review-baseline-explain.sql > docs/evidence/phase-06/review-aggregate/baseline/explain.txt

docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/12-review-naive-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/13-review-naive-explain.sql > docs/evidence/phase-06/review-aggregate/naive-index/explain.txt

docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/14-review-query-shaped-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/15-review-query-shaped-explain.sql > docs/evidence/phase-06/review-aggregate/query-shaped-index/explain.txt
```

Monthly Order Aggregate:

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/20-monthly-baseline-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/21-monthly-baseline-explain.sql > docs/evidence/phase-06/monthly-order-aggregate/baseline/explain.txt

docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/22-monthly-naive-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/23-monthly-naive-explain.sql > docs/evidence/phase-06/monthly-order-aggregate/naive-index/explain.txt

docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/24-monthly-query-shaped-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/25-monthly-query-shaped-explain.sql > docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt
```

## API test

Run from `ecommerce/`:

```bash
rtk gradlew test --tests "*ProductReviewSummaryTest"
```

## k6 evidence

Run naive index API evidence:

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/12-review-naive-prepare.sql
K6_LOG_FILE=docs/evidence/phase-06/review-summary-api/naive-index/k6-summary.txt K6_RUN_WINDOW_FILE=docs/evidence/phase-06/review-summary-api/naive-index/run-window.json PHASE=phase-06 POOL=pool10 ./k6/run.sh review-summary review-summary-baseline prometheus
```

Run query-shaped index API evidence:

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/14-review-query-shaped-prepare.sql
K6_LOG_FILE=docs/evidence/phase-06/review-summary-api/query-shaped-index/k6-summary.txt K6_RUN_WINDOW_FILE=docs/evidence/phase-06/review-summary-api/query-shaped-index/run-window.json PHASE=phase-06 POOL=pool10 ./k6/run.sh review-summary review-summary-baseline prometheus
```
```

- [ ] **Step 5: Add Phase 6 observability doc**

Create `docs/phases/06-aggregation/observability.md`:

```markdown
# Phase 6 Observability

## Primary evidence

| Evidence | Purpose |
|---|---|
| `EXPLAIN (ANALYZE, BUFFERS)` | Compare scan, join, sort, aggregate nodes and execution time |
| data profile | Explain planner choices using row counts and distribution |
| `pg_stat_statements` reset per condition | Keep query-level snapshots separated |
| k6 summary | Compare representative API p95/p99 for naive vs query-shaped index |
| Grafana screenshot | Check Hikari and DB scan/index scan trends for API evidence |

## SQL plan interpretation

- Record `Planning Time` and `Execution Time`.
- Record shared hit/read buffers.
- Record scan node: `Seq Scan`, `Index Scan`, `Bitmap Index Scan`, `Bitmap Heap Scan`.
- Record aggregate node: `HashAggregate` or `GroupAggregate`.
- Record `Sort` presence and `Sort Method`.
- Do not assume query-shaped index forces `GroupAggregate`.

## API interpretation

k6 is secondary evidence. It checks whether practical candidate indexes affect API p95/p99. It does not replace SQL-only plan evidence.
```

- [ ] **Step 6: Add initial Phase 6 report**

Create `docs/phases/06-aggregation/report.md`:

```markdown
# Phase 6 결과 보고서

## 상태

진행 전.

## Data Profile

Phase 6 측정 전 row count, review 분포, monthly order 분포, index state를 기록한다.

## Product Review Summary

| Condition | Plan Summary | Execution Time | Evidence |
|---|---|---:|---|
| baseline | 미측정 | 미측정 | `docs/evidence/phase-06/review-aggregate/baseline/explain.txt` |
| naive index | 미측정 | 미측정 | `docs/evidence/phase-06/review-aggregate/naive-index/explain.txt` |
| query-shaped index | 미측정 | 미측정 | `docs/evidence/phase-06/review-aggregate/query-shaped-index/explain.txt` |

## Monthly Order Aggregate

| Condition | Plan Summary | Execution Time | Evidence |
|---|---|---:|---|
| baseline | 미측정 | 미측정 | `docs/evidence/phase-06/monthly-order-aggregate/baseline/explain.txt` |
| naive index | 미측정 | 미측정 | `docs/evidence/phase-06/monthly-order-aggregate/naive-index/explain.txt` |
| query-shaped index | 미측정 | 미측정 | `docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt` |

## Representative API Evidence

| Condition | k6 p95 | k6 p99 | Evidence |
|---|---:|---:|---|
| naive index | 미측정 | 미측정 | `docs/evidence/phase-06/review-summary-api/naive-index/k6-summary.txt` |
| query-shaped index | 미측정 | 미측정 | `docs/evidence/phase-06/review-summary-api/query-shaped-index/k6-summary.txt` |

## Phase 7 Handoff

Phase 6은 집계 쿼리 병목을 다룬다. 대량 이력 테이블의 깊은 페이지 조회 병목은 Phase 7 pagination 실험으로 분리한다.
```

- [ ] **Step 7: Add Phase 6 evidence README**

Create `docs/evidence/phase-06/README.md`:

```markdown
# Phase 06 Evidence

Phase 6 evidence는 PostgreSQL 집계 쿼리에서 baseline, naive index, query-shaped index condition의 실행계획과 실행시간을 비교한다.

## Data Profile

| Evidence | 경로 | 목적 |
|---|---|---|
| Row counts | [data-profile/row-counts.txt](./data-profile/row-counts.txt) | 대상 테이블 규모 |
| Review distribution | [data-profile/review-distribution.txt](./data-profile/review-distribution.txt) | Product별 review 분포 |
| Monthly order distribution | [data-profile/monthly-order-distribution.txt](./data-profile/monthly-order-distribution.txt) | 월별 order 분포 |
| Index state | [data-profile/index-state-before.txt](./data-profile/index-state-before.txt) | 측정 전 index 상태 |

## Product Review Summary

| Condition | Evidence |
|---|---|
| baseline | [review-aggregate/baseline/explain.txt](./review-aggregate/baseline/explain.txt) |
| naive index | [review-aggregate/naive-index/explain.txt](./review-aggregate/naive-index/explain.txt) |
| query-shaped index | [review-aggregate/query-shaped-index/explain.txt](./review-aggregate/query-shaped-index/explain.txt) |

## Monthly Order Aggregate

| Condition | Evidence |
|---|---|
| baseline | [monthly-order-aggregate/baseline/explain.txt](./monthly-order-aggregate/baseline/explain.txt) |
| naive index | [monthly-order-aggregate/naive-index/explain.txt](./monthly-order-aggregate/naive-index/explain.txt) |
| query-shaped index | [monthly-order-aggregate/query-shaped-index/explain.txt](./monthly-order-aggregate/query-shaped-index/explain.txt) |

## Product Review Summary API

| Condition | k6 summary | run window |
|---|---|---|
| naive index | [review-summary-api/naive-index/k6-summary.txt](./review-summary-api/naive-index/k6-summary.txt) | [review-summary-api/naive-index/run-window.json](./review-summary-api/naive-index/run-window.json) |
| query-shaped index | [review-summary-api/query-shaped-index/k6-summary.txt](./review-summary-api/query-shaped-index/k6-summary.txt) | [review-summary-api/query-shaped-index/run-window.json](./review-summary-api/query-shaped-index/run-window.json) |
```

- [ ] **Step 8: Verify scaffold**

Run:

```bash
rtk powershell -NoProfile -Command "$paths = @('docs/phases/06-aggregation/README.md','docs/phases/06-aggregation/scope.md','docs/phases/06-aggregation/runbook.md','docs/phases/06-aggregation/observability.md','docs/phases/06-aggregation/report.md','docs/evidence/phase-06/README.md'); $paths | ForEach-Object { if (-not (Test-Path $_)) { throw \"Missing $_\" } }"
```

Expected: command exits 0.

- [ ] **Step 9: Commit**

```bash
git add docs/phases/06-aggregation docs/evidence/phase-06/README.md
git commit -m "docs(phase6): scaffold aggregation phase docs"
```

