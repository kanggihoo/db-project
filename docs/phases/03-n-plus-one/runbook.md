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

## BatchSize profile

BatchSize evidence must run with the isolated profile:

```bash
./scripts/server.sh phase3-batch
PHASE=phase-03 POOL=pool10 STRATEGY=batch-size ./k6/run.sh orders baseline prometheus
```

Lazy, Fetch Join, and EntityGraph evidence must run without the `phase3-batch` profile so global batch fetching does not affect their SQL count.

## Strategy evidence capture

For each strategy, reset statistics, run k6, then capture `pg_stat_statements`. Use the default server profile for `lazy`, `fetch-join`, and `entity-graph`; restart the server with `phase3-batch` only for `batch-size`.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/00-reset-statistics.sql
PHASE=phase-03 POOL=pool10 STRATEGY=lazy K6_LOG_FILE=docs/evidence/phase-03/orders/lazy/k6-summary.txt ./k6/run.sh orders baseline prometheus
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/01-pg-stat-statements.sql | tee docs/evidence/phase-03/orders/lazy/pg-stat-statements.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce -v user_id=100 < scripts/phase-03/02-order-shape.sql | tee docs/evidence/phase-03/orders/lazy/sql-count.txt
```

Repeat with `STRATEGY=fetch-join` and `STRATEGY=entity-graph` under the default server profile, changing the output directory to match the strategy.

For BatchSize, restart the server with the isolated profile before running the same capture sequence:

```bash
./scripts/server.sh phase3-batch
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/00-reset-statistics.sql
PHASE=phase-03 POOL=pool10 STRATEGY=batch-size K6_LOG_FILE=docs/evidence/phase-03/orders/batch-size/k6-summary.txt ./k6/run.sh orders baseline prometheus
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/01-pg-stat-statements.sql | tee docs/evidence/phase-03/orders/batch-size/pg-stat-statements.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce -v user_id=100 < scripts/phase-03/02-order-shape.sql | tee docs/evidence/phase-03/orders/batch-size/sql-count.txt
```

## Representative EXPLAIN

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/10-lazy-explain.sql | tee docs/evidence/phase-03/orders/lazy/explain.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/20-fetch-join-explain.sql | tee docs/evidence/phase-03/orders/fetch-join/explain.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/30-batch-size-explain.sql | tee docs/evidence/phase-03/orders/batch-size/explain.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-03/40-entity-graph-explain.sql | tee docs/evidence/phase-03/orders/entity-graph/explain.txt
```
