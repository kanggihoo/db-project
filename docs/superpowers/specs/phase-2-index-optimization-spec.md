# Phase 2 Index Optimization Spec

> Phase 1 상품 검색 Baseline을 기준으로 `product(category_id, status)` 필터 쿼리의 인덱스 적용 전후 차이를 증거로 남기기 위한 요구사항이다.

## Goal

Phase 2는 Phase 1의 `GET /api/products?categoryId=&status=` API 흐름을 유지한 상태에서, PostgreSQL 인덱스 설계가 실행계획과 성능 지표를 어떻게 바꾸는지 확인한다.

메인 실험은 Phase 1의 상품 검색 API가 발생시키는 SQL shape인 `product where category_id = ? and status = ?`를 대상으로 한다. 비교 기준은 Phase 1 Evidence의 `products/pool10-baseline` 결과이며, 같은 `loadtest`, `pool10`, `products baseline` 조건으로 post-change evidence를 수집한다.

이 spec은 [Phase 2 roadmap](../../roadmap/03-phase-2-indexes.md), [CONTEXT.md](../../../CONTEXT.md), [ADR 0002](../../adr/0002-require-phase-evidence-under-docs-evidence.md), [ADR 0004](../../adr/0004-keep-naive-implementations-as-deliberate-baselines.md), [ADR 0005](../../adr/0005-use-one-observability-dashboard-for-comparable-phase-evidence.md)를 따른다.

## Non-Goals

- Phase 1의 API, Controller, Service, Repository 흐름을 성능 개선 목적으로 바꾸지 않는다.
- DTO projection, QueryDSL, fetch join, batch loading 같은 애플리케이션 쿼리 최적화는 포함하지 않는다.
- Flyway/Liquibase 같은 운영 migration 절차는 포함하지 않는다. 운영 중 스키마 변경 절차는 Phase 10 주제로 남긴다.
- SQL-only 보조 실험 결과를 Phase 1 API 성능 결과와 직접 비교하지 않는다.
- 상품 검색을 production ecommerce feature로 확장하지 않는다.
- 주문 목록 N+1, 포인트 pagination 병목을 Phase 2에서 해결하지 않는다.

## Decisions

### Preserve The Phase 1 API Path

메인 실험은 Phase 1 Baseline과 비교 가능해야 하므로 기존 API path를 유지한다.

| Layer | Decision |
|---|---|
| API | `GET /api/products?categoryId=&status=` 유지 |
| Service | `ProductService.searchProducts` 흐름 유지 |
| Repository | `findByCategoryIdAndStatus` 유지 |
| SQL shape | `product where category_id = ? and status = ?` 유지 |
| Change target | PostgreSQL index와 evidence 문서 |

### Use A Scripted Experimental Index

Phase 2의 메인 인덱스는 실험용 SQL 스크립트로 관리한다.

```sql
CREATE INDEX idx_product_category_status
  ON product(category_id, status);
```

이 선택은 Phase 2가 운영 배포 절차가 아니라 Learning Phase이기 때문이다. 인덱스 생성, 삭제, 재실행이 쉬워야 하며, migration 도구 도입은 Phase 10의 학습 목표로 남긴다.

### Separate Main API Comparison From SQL-only Experiments

메인 실험은 API, k6, Grafana, `pg_stat_statements`, `EXPLAIN (ANALYZE, BUFFERS)`를 함께 사용한다.

SQL-only 보조 실험은 `psql`로 직접 실행한다. 보조 실험은 인덱스 원리를 확인하기 위한 것이며, API latency나 k6 결과와 섞어 해석하지 않는다.

| Experiment Type | Entry Point | Purpose | Evidence |
|---|---|---|---|
| Main API comparison | Existing product API | Phase 1 대비 성능 변화 확인 | k6, Grafana, `pg_stat_statements`, EXPLAIN |
| SQL-only auxiliary | `psql` SQL | 인덱스 원리 확인 | EXPLAIN output, notes |

## Requirements

### Main Experiment

The implementation plan must produce a repeatable main experiment.

1. Capture pre-index `EXPLAIN (ANALYZE, BUFFERS)` for the product search SQL.
2. Create `idx_product_category_status` on `product(category_id, status)`.
3. Capture post-index `EXPLAIN (ANALYZE, BUFFERS)` for the same SQL.
4. Run k6 under the same measurement condition as Phase 1: `loadtest`, `pool10`, `products`, `baseline`, 50 rps.
5. Capture `pg_stat_statements` after the run.
6. Capture Grafana evidence from `DB Lab Overview`.
7. Compare against Phase 1 values.

Phase 1 comparison baseline:

| Metric | Value |
|---|---:|
| API p95 | 17.24ms |
| SQL mean time | 7.90ms |
| SQL total time | 118,489.64ms |
| Requests | 15,001 |
| Failed | 0.00% |
| Dropped iterations | 0 |

### SQL-only Auxiliary Experiments

The implementation plan must include SQL-only experiments for the following concepts.

| Topic | Required Question |
|---|---|
| Single-column index selectivity | Does `product(status)` help when `status = 'ON_SALE'` has low selectivity? |
| Composite index order | How do leftmost column, range condition, and `ORDER BY` change index usefulness? |
| Covering index | Can a SQL query that selects only indexed columns reduce table access or reach Index Only Scan? |
| Partial index | How does `WHERE is_deleted = false` change index design and plan selection? |

Each auxiliary experiment must isolate indexes. Before moving to another experiment, remove unrelated experimental indexes and refresh planner statistics when needed.

### Phase Documentation

The implementation plan must create the standard Phase 2 documentation set under `docs/phases/02-indexes/`.

| File | Required Content |
|---|---|
| `README.md` | Phase status, source documents, evidence links |
| `scope.md` | 목표, 대상 API/table, non-goals, completion criteria |
| `runbook.md` | DB 준비, index create/drop, EXPLAIN, k6, evidence capture commands |
| `observability.md` | `EXPLAIN`, `pg_stat_statements`, k6, Grafana 해석 기준 |
| `report.md` | pre/post 결과, 보조 실험 해석, Phase 3로 넘길 근거 |

### Evidence Layout

Raw evidence must be stored under `docs/evidence/phase-02/`.

Expected evidence categories:

- Main product API pre-index EXPLAIN output
- Main product API post-index EXPLAIN output
- k6 summary for post-index product baseline
- `pg_stat_statements` snapshot for post-index product baseline
- Grafana screenshot for product index comparison
- SQL-only auxiliary EXPLAIN outputs
- Phase 2 evidence README or index

## Acceptance Criteria

- Phase 2 documentation exists under `docs/phases/02-indexes/` and follows the standard Phase document structure.
- The main experiment uses the existing product API path without application-layer optimization.
- The main index is `idx_product_category_status ON product(category_id, status)`.
- Pre-index and post-index `EXPLAIN (ANALYZE, BUFFERS)` outputs for the representative product query are recorded.
- Post-index k6 is run with the same measurement condition used by Phase 1 product baseline.
- Post-index `pg_stat_statements` and Grafana evidence are recorded under `docs/evidence/phase-02/`.
- The Phase 2 report compares Phase 1 baseline values with Phase 2 post-index values.
- SQL-only auxiliary experiments are documented separately from the main API comparison.
- The report states whether the remaining bottleneck belongs to query count/N+1 and can move to Phase 3.

## Open Questions

- Should Phase 2 keep all index changes as manual SQL scripts only, or also include a rollback helper script for repeatability?
- Should SQL-only auxiliary experiments be stored as one consolidated SQL file or separate files per topic?
- Should the post-index k6 run reuse the `baseline` preset only, or also include `stress-100` as optional evidence?
