# Phase 2 실행 절차

> 목적: Phase 1 상품 검색 Baseline과 같은 조건에서 인덱스 적용 전후 Evidence를 재현 가능하게 수집한다.

## 전체 흐름

```text
1. Docker 인프라를 기동한다.
2. loadtest 데이터로 시딩한다.
3. Spring 서버를 pool10 설정으로 실행한다.
4. 실험용 product 인덱스를 정리한다.
5. 메인 상품 검색 SQL의 pre-index EXPLAIN을 저장한다.
6. idx_product_category_status 인덱스를 생성한다.
7. 메인 상품 검색 SQL의 post-index EXPLAIN을 저장한다.
8. products baseline k6를 phase-02, pool10 조건으로 실행한다.
9. post-index pg_stat_statements와 Grafana screenshot을 저장한다.
10. SQL-only 보조 실험을 실행하고 report.md를 갱신한다.
```

## 사전 조건

프로젝트 루트에서 실행한다.

```bash
rtk docker compose up -d
./scripts/seed.sh loadtest
./scripts/server.sh pool10
```

## 메인 API 비교 실험

실험 시작 전에 이전 실험 인덱스와 통계를 정리한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/00-clean-product-indexes.sql
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "VACUUM ANALYZE product;"
```

인덱스 적용 전 실행계획을 저장한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/01-main-pre-index-explain.sql | tee docs/evidence/phase-02/products/pre-index/explain.txt
```

메인 인덱스를 생성한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/02-create-main-index.sql
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "VACUUM ANALYZE product;"
```

인덱스 적용 후 실행계획을 저장한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/03-main-post-index-explain.sql | tee docs/evidence/phase-02/products/pool10-post-index/explain.txt
```

Phase 1과 같은 `products baseline`, `pool10`, 50 rps 조건으로 k6를 실행한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
PHASE=phase-02 POOL=pool10 ./k6/run.sh products baseline prometheus | tee docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
```

`pg_stat_statements` snapshot을 저장한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/04-product-pg-stat-statements.sql | tee docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

## SQL-only 보조 실험

보조 실험은 API/k6 latency와 직접 비교하지 않는다. 각 스크립트는 필요한 실험 인덱스만 남긴 상태에서 실행한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/10-single-status-index.sql | tee docs/evidence/phase-02/sql-only/single-status-index.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/20-composite-order-index.sql | tee docs/evidence/phase-02/sql-only/composite-order-index.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/30-covering-index.sql | tee docs/evidence/phase-02/sql-only/covering-index.txt
rtk docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-02/40-partial-index.sql | tee docs/evidence/phase-02/sql-only/partial-index.txt
```

## Grafana 캡처

Grafana는 공통 `DB Lab Overview` dashboard를 사용한다.

```bash
PHASE=phase-02
SCENARIO=products
PRESET=baseline
POOL=pool10
SCREENSHOT=docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

저장할 screenshot 경로:

```text
docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

## 보고서 갱신

Evidence 수집 후 [report.md](./report.md)에 다음 값을 기록한다.

- Phase 1 기준값과 Phase 2 post-index 측정값 비교
- `EXPLAIN (ANALYZE, BUFFERS)`의 scan type, buffer, execution time 변화
- `pg_stat_statements` mean/total/calls/rows 변화
- k6 p95, failed, dropped_iterations 변화
- SQL-only 보조 실험 해석
- Phase 3으로 넘길 반복 SQL/query-count 병목
