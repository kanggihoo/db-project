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
10. SQL-only 보조 실험을 실행한다.
```

## 사전 조건

프로젝트 루트에서 실행한다. Docker 또는 OrbStack daemon이 떠 있어야 한다.

```bash
docker info
docker compose config --quiet
docker compose up -d postgres postgres_exporter prometheus grafana
./scripts/seed.sh loadtest
./scripts/server.sh pool10
```

서버는 별도 터미널에서 계속 실행한다. k6는 `host.docker.internal:8080`으로 Spring 서버에 접근한다.

## DB 상태 확인

seed 직후 DB 상태를 저장한다.

```bash
make phase-sql \
  FILE=scripts/db-state/00-seed-state.sql \
  OUTPUT=docs/evidence/phase-02/db-state/00-seed-loadtest-state.txt
```

실험용 product index를 정리한 뒤 pre-index 상태를 저장한다.

```bash
make phase-sql \
  FILE=scripts/phase-02/00-clean-product-indexes.sql

docker compose exec -T postgres psql -U app -d ecommerce -c "VACUUM ANALYZE product;"

make phase-sql \
  FILE=scripts/db-state/00-seed-state.sql \
  OUTPUT=docs/evidence/phase-02/db-state/10-pre-index-product-state.txt
```

## 메인 API 비교 실험

pre-index EXPLAIN을 저장한다.

```bash
make phase-sql \
  FILE=scripts/phase-02/01-main-pre-index-explain.sql \
  OUTPUT=docs/evidence/phase-02/products/pre-index/explain.txt
```

post-index 준비와 EXPLAIN을 저장한다.

```bash
make phase-sql \
  FILE=scripts/phase-02/02-create-main-index.sql

docker compose exec -T postgres psql -U app -d ecommerce -c "VACUUM ANALYZE product;"

make phase-sql \
  FILE=scripts/db-state/00-seed-state.sql \
  OUTPUT=docs/evidence/phase-02/db-state/20-post-index-product-state.txt

make phase-sql \
  FILE=scripts/phase-02/03-main-post-index-explain.sql \
  OUTPUT=docs/evidence/phase-02/products/pool10-post-index/explain.txt
```

k6 evidence wrapper로 실행한다.

```bash
docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"

make evidence-capture \
  PHASE=phase-02 \
  SCENARIO=products \
  PRESET=baseline \
  CONDITION=pool10-post-index \
  TABLE=product \
  OUTPUT=docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

생성 파일:

```text
docs/evidence/phase-02/products/pool10-post-index/measurement.json
docs/evidence/phase-02/products/pool10-post-index/k6-summary.json
docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
docs/evidence/phase-02/products/pool10-post-index/k6-exit-status.txt
docs/evidence/phase-02/products/pool10-post-index/run-window.json
docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

post-index `pg_stat_statements`를 저장한다.

```bash
make phase-sql \
  FILE=scripts/phase-02/04-product-pg-stat-statements.sql \
  OUTPUT=docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

## SQL-only 보조 실험

```bash
make phase-sql \
  FILE=scripts/phase-02/10-single-status-index.sql \
  OUTPUT=docs/evidence/phase-02/sql-only/single-status-index.txt

make phase-sql \
  FILE=scripts/phase-02/20-composite-order-index.sql \
  OUTPUT=docs/evidence/phase-02/sql-only/composite-order-index.txt

make phase-sql \
  FILE=scripts/phase-02/30-covering-index.sql \
  OUTPUT=docs/evidence/phase-02/sql-only/covering-index.txt

make phase-sql \
  FILE=scripts/phase-02/40-partial-index.sql \
  OUTPUT=docs/evidence/phase-02/sql-only/partial-index.txt
```

## Evidence 파일 검증

```bash
test -f docs/evidence/phase-02/products/pool10-post-index/measurement.json
test -f docs/evidence/phase-02/products/pool10-post-index/k6-summary.json
test -f docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
test -f docs/evidence/phase-02/products/pool10-post-index/k6-exit-status.txt
test -f docs/evidence/phase-02/products/pool10-post-index/run-window.json
test -f docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
test -f docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

```bash
node -e "const fs=require('fs'); const m=JSON.parse(fs.readFileSync('docs/evidence/phase-02/products/pool10-post-index/measurement.json','utf8')); if (m.phase !== 'phase-02' || m.scenario !== 'products' || m.preset !== 'baseline' || m.pool !== 'pool10' || m.condition !== 'pool10-post-index') process.exit(1); console.log('phase-02 measurement ok');"
```
