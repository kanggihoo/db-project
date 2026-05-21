# 001 Phase Documentation Scaffold

## Goal

Create the Phase 2 documentation and evidence directories before running database experiments.

## Files

- Create: `docs/phases/02-indexes/README.md`
- Create: `docs/phases/02-indexes/scope.md`
- Create: `docs/phases/02-indexes/runbook.md`
- Create: `docs/phases/02-indexes/observability.md`
- Create: `docs/phases/02-indexes/report.md`
- Create: `docs/evidence/phase-02/README.md`
- Modify: `docs/superpowers/README.md`

## Steps

- [ ] **Step 1: Create the Phase 2 directory structure**

Run:

```bash
rtk proxy mkdir -p docs/phases/02-indexes docs/evidence/phase-02/products/pre-index docs/evidence/phase-02/products/pool10-post-index docs/evidence/phase-02/sql-only docs/evidence/phase-02/grafana-screenshots
```

Expected: command exits 0.

- [ ] **Step 2: Create `docs/phases/02-indexes/README.md`**

Use this content:

```markdown
# Phase 2. 인덱스 설계와 실행계획 분석

Phase 2는 Phase 1의 상품 검색 API를 그대로 두고, `product(category_id, status)` 필터 쿼리에 인덱스를 적용했을 때 실행계획과 측정 지표가 어떻게 바뀌는지 확인하는 Learning Phase다.

## 현재 상태

Phase 2는 준비 중이다.

- Phase 1 `products/pool10-baseline` 결과를 비교 기준으로 사용한다.
- 메인 실험은 기존 `GET /api/products?categoryId=&status=` API 경로를 사용한다.
- SQL-only 보조 실험은 `psql`과 `EXPLAIN (ANALYZE, BUFFERS)`로 별도 수행한다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 2 범위와 제외 범위 |
| [runbook.md](./runbook.md) | 반복 가능한 실행 절차 |
| [observability.md](./observability.md) | 실행계획과 지표 해석 기준 |
| [report.md](./report.md) | 측정 결과와 Phase 3 handoff |

## Source Documents

- Roadmap: [docs/roadmap/03-phase-2-indexes.md](../../roadmap/03-phase-2-indexes.md)
- Spec: [docs/superpowers/specs/phase-2-index-optimization-spec.md](../../superpowers/specs/phase-2-index-optimization-spec.md)
- Plan: [docs/superpowers/plans/phase-2-index-optimization/000-plan-index.md](../../superpowers/plans/phase-2-index-optimization/000-plan-index.md)
- Phase 1 baseline: [docs/evidence/phase-01/BASELINE.md](../../evidence/phase-01/BASELINE.md)

## 관련 산출물

- Phase Evidence: [docs/evidence/phase-02/README.md](../../evidence/phase-02/README.md)
- 공통 k6 가이드: [docs/guides/k6-load-testing.md](../../guides/k6-load-testing.md)
- 공통 Grafana 가이드: [docs/guides/grafana-observability.md](../../guides/grafana-observability.md)

## 다음 Phase 연결

Phase 2 이후에도 주문 목록 조회가 느리다면 원인은 인덱스가 아니라 쿼리 수 문제로 분류하고 Phase 3 N+1 최적화로 넘긴다.
```

- [ ] **Step 3: Create `docs/phases/02-indexes/scope.md`**

Use this content:

```markdown
# Phase 2 범위

## 목표

Phase 2의 목표는 Phase 1 상품 검색 Baseline과 비교 가능한 Post-change Evidence를 남기는 것이다.

- 기존 상품 검색 API가 발생시키는 `product(category_id, status)` 필터 쿼리의 pre-index 실행계획을 기록한다.
- `idx_product_category_status ON product(category_id, status)`를 적용한다.
- post-index 실행계획, k6 결과, `pg_stat_statements`, Grafana screenshot을 기록한다.
- SQL-only 보조 실험으로 단일 인덱스 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스를 확인한다.

## 대상 API와 테이블

| 대상 | 값 |
|---|---|
| API | `GET /api/products?categoryId=&status=` |
| Repository | `ProductRepository.findByCategoryIdAndStatus` |
| Table | `product` |
| Main query shape | `product where category_id = ? and status = ?` |
| Main index | `idx_product_category_status ON product(category_id, status)` |

## 비교 기준

| Metric | Phase 1 Value |
|---|---:|
| k6 scenario | `products` |
| preset | `baseline` |
| pool | `pool10` |
| request rate | 50 rps |
| API p95 | 17.24ms |
| SQL mean time | 7.90ms |
| SQL total time | 118,489.64ms |
| requests | 15,001 |
| failed | 0.00% |

## 제외 범위

- API, Controller, Service, Repository를 성능 개선 목적으로 바꾸지 않는다.
- DTO projection, QueryDSL, fetch join, batch loading은 적용하지 않는다.
- Flyway/Liquibase migration은 적용하지 않는다.
- SQL-only 보조 실험 결과를 API/k6 결과와 직접 비교하지 않는다.
- 주문 목록 N+1과 포인트 pagination은 해결하지 않는다.

## 완료 조건

- [ ] pre-index `EXPLAIN (ANALYZE, BUFFERS)`가 저장됐다.
- [ ] `idx_product_category_status` 적용 후 post-index `EXPLAIN (ANALYZE, BUFFERS)`가 저장됐다.
- [ ] `PHASE=phase-02 POOL=pool10 ./k6/run.sh products baseline prometheus` 결과가 저장됐다.
- [ ] post-index `pg_stat_statements` snapshot이 저장됐다.
- [ ] Grafana screenshot이 저장됐다.
- [ ] SQL-only 보조 실험 결과가 메인 API 비교와 분리되어 저장됐다.
- [ ] `report.md`가 Phase 1 대비 Phase 2 결과와 Phase 3 handoff를 기록했다.
```

- [ ] **Step 4: Create `docs/phases/02-indexes/runbook.md`**

Use this content:

````markdown
# Phase 2 실행 절차

## 전체 흐름

```text
1. Docker 인프라와 loadtest 데이터 준비
2. 실험용 product 인덱스 정리
3. pre-index EXPLAIN 저장
4. main index 생성
5. post-index EXPLAIN 저장
6. Spring 서버 pool10 실행
7. k6 products baseline 실행
8. pg_stat_statements와 Grafana evidence 저장
9. SQL-only 보조 실험 실행
10. report.md 작성
```

## 사전 조건

```bash
rtk docker compose up -d
./scripts/seed.sh loadtest
./scripts/server.sh pool10
```

서버는 별도 터미널에서 유지한다.

## 메인 실험

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/00-clean-product-indexes.sql
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/01-main-pre-index-explain.sql | tee docs/evidence/phase-02/products/pre-index/explain.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/02-create-main-index.sql
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/03-main-post-index-explain.sql | tee docs/evidence/phase-02/products/pool10-post-index/explain.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "VACUUM ANALYZE product;"
PHASE=phase-02 POOL=pool10 ./k6/run.sh products baseline prometheus | tee docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/04-product-pg-stat-statements.sql | tee docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

## SQL-only 보조 실험

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/10-single-status-index.sql | tee docs/evidence/phase-02/sql-only/single-status-index.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/20-composite-order-index.sql | tee docs/evidence/phase-02/sql-only/composite-order-index.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/30-covering-index.sql | tee docs/evidence/phase-02/sql-only/covering-index.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/40-partial-index.sql | tee docs/evidence/phase-02/sql-only/partial-index.txt
```

## Grafana 캡처

Grafana에서 `DB Lab Overview`를 열고 다음 변수로 맞춘다.

| Variable | Value |
|---|---|
| `$phase` | `phase-02` |
| `$scenario` | `products` |
| `$preset` | `baseline` |
| `$pool` | `pool10` |

Run Summary, Table Access, Phase 2 Index Focus가 보이도록 screenshot을 저장한다.

```text
docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```
````

- [ ] **Step 5: Create `docs/phases/02-indexes/observability.md`**

Use this content:

```markdown
# Phase 2 관측 전략

## EXPLAIN

`EXPLAIN (ANALYZE, BUFFERS)`에서 다음 항목을 본다.

| 항목 | 해석 |
|---|---|
| `Seq Scan` | 테이블을 순차적으로 읽는다. pre-index 기준선에서 기대된다. |
| `Index Scan` | 인덱스로 후보 row를 찾고 heap row를 읽는다. |
| `Bitmap Index Scan` | 여러 후보를 bitmap으로 모은 뒤 heap block을 읽는다. |
| `Index Only Scan` | 인덱스만으로 결과를 만들 수 있는 경우 가능하다. visibility map 상태의 영향을 받는다. |
| `Buffers` | shared hit/read block 수로 읽기 비용을 비교한다. |
| `Execution Time` | 단일 SQL 실행시간이다. k6 latency와 직접 같은 값은 아니다. |

## pg_stat_statements

post-index k6 실행 직후 product query shape의 `calls`, `mean_exec_time`, `total_exec_time`, `rows`를 확인한다.

Phase 1 기준:

| Query | Calls | Mean | Total | Rows |
|---|---:|---:|---:|---:|
| `product where category_id = ? and status = ?` | 15,001 | 7.90ms | 118,489.64ms | 2,508,501 |

## k6

`products baseline`의 `http_req_duration p95`, `http_req_failed`, `dropped_iterations`를 Phase 1과 비교한다.

## Grafana

`DB Lab Overview`에서 다음을 본다.

- Run Summary: p95, error rate, RPS, dropped iterations
- Table Access: `product`의 seq scan과 index scan 변화
- Hikari Pool: product API가 connection pool 병목을 만들지 않는지 확인

## SQL-only 보조 실험 해석

SQL-only 결과는 API 성능 개선의 증거가 아니다. 각 실험은 인덱스 개념을 확인하는 보조 evidence로만 해석한다.
```

- [ ] **Step 6: Create `docs/phases/02-indexes/report.md`**

Use this initial content:

```markdown
# Phase 2 결과 보고서

## 결론

Phase 2 측정 전이다. 이 문서는 메인 인덱스 실험과 SQL-only 보조 실험이 끝난 뒤 업데이트한다.

## 비교 기준

| Metric | Phase 1 Baseline |
|---|---:|
| API p95 | 17.24ms |
| SQL mean time | 7.90ms |
| SQL total time | 118,489.64ms |
| requests | 15,001 |
| failed | 0.00% |
| dropped iterations | 0 |

## 메인 실험 결과

측정 후 pre-index plan, post-index plan, k6 summary, `pg_stat_statements`, Grafana screenshot을 근거로 작성한다.

## SQL-only 보조 실험 결과

측정 후 단일 인덱스 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스 결과를 메인 API 비교와 분리해 작성한다.

## Phase 3 Handoff

상품 검색 인덱스 실험 후 남는 주요 병목이 주문 목록의 반복 SQL이면 Phase 3 N+1 최적화로 넘긴다.
```

- [ ] **Step 7: Create `docs/evidence/phase-02/README.md`**

Use this content:

```markdown
# Phase 2 Evidence

Phase 2 evidence는 상품 검색 API의 인덱스 적용 전후 비교와 SQL-only 보조 실험 결과를 보관한다.

## Main API Comparison

| Evidence | Path |
|---|---|
| Pre-index EXPLAIN | `products/pre-index/explain.txt` |
| Post-index EXPLAIN | `products/pool10-post-index/explain.txt` |
| Post-index k6 summary | `products/pool10-post-index/k6-summary.txt` |
| Post-index pg_stat_statements | `products/pool10-post-index/pg-stat-statements.txt` |
| Grafana screenshot | `grafana-screenshots/products-post-index.png` |

## SQL-only Auxiliary Experiments

| Topic | Path |
|---|---|
| Single status index | `sql-only/single-status-index.txt` |
| Composite index order | `sql-only/composite-order-index.txt` |
| Covering index | `sql-only/covering-index.txt` |
| Partial index | `sql-only/partial-index.txt` |
```

- [ ] **Step 8: Update `docs/superpowers/README.md`**

Add this line under current artifacts:

```markdown
- [Phase 2 Index Optimization Plan](./plans/phase-2-index-optimization/000-plan-index.md)
```

- [ ] **Step 9: Verify scaffold**

Run:

```bash
rtk find docs/phases/02-indexes -maxdepth 1 -type f
rtk read docs/evidence/phase-02/README.md
rtk grep "Phase 2 Index Optimization Plan" docs/superpowers/README.md
```

Expected:

- `README.md`, `scope.md`, `runbook.md`, `observability.md`, and `report.md` exist under `docs/phases/02-indexes/`.
- `docs/evidence/phase-02/README.md` is readable.
- `docs/superpowers/README.md` links the plan.

- [ ] **Step 10: Commit**

```bash
git add docs/phases/02-indexes docs/evidence/phase-02/README.md docs/superpowers/README.md
git commit -m "docs: scaffold phase 2 index documentation"
```
