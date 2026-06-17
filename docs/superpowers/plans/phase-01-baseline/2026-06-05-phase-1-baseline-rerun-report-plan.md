# Phase 1 Baseline 재측정 및 Report 작성 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**목표:** 빈 `docs/evidence/phase-01/` 상태에서 Phase 1 baseline evidence를 처음부터 다시 수집하고, 결과 타당성 판단 후 `docs/phases/01-baseline/report.md`를 새 evidence 기준으로 작성한다.

**구조:** Phase 1 코드는 이미 구현된 것으로 보고, evidence 재현성에 필요한 SQL 파일과 k6 measurement manifest만 보강한다. 수집 흐름은 fresh `loadtest` seed, DB 상태 확인, API smoke, SQL 실행계획, k6/Grafana, `pg_stat_statements`, 결과 타당성 판단, 최종 보고서 작성 순서로 진행한다.

**기술 스택:** Spring Boot, PostgreSQL, Docker Compose, k6, Prometheus, Grafana, Node.js evidence scripts, Makefile targets.

---

## 중요한 전제

- 대상 Phase 문서: `docs/phases/01-baseline/`
- Roadmap: `docs/roadmap/02-phase-1-baseline.md`
- Evidence root: `docs/evidence/phase-01/`
- 기준 pool: `pool10`
- 기준 seed: `loadtest`
- 기준 k6 mode: `prometheus`
- k6 최종 수치 원본: `k6-summary.json`
- `k6-summary.txt`는 더 이상 필수 evidence가 아니다.
- 현재 `scripts/run-k6-evidence.mjs`는 `measurement.json`을 생성하지 않는다. Phase evidence rule을 만족하려면 k6 실행 전에 이 gap을 먼저 해소해야 한다.

## 최종 evidence 구조

```text
docs/evidence/phase-01/
  README.md
  db/
    before-measurement-state.txt
    after-measurement-state.txt
  api-smoke/
    spring-health.json
    products-baseline.json
    orders-lazy-user950.json
    points-page0-user5714.json
    points-page500-user5714.json
  explain/
    products-baseline-explain.txt
    orders-lazy-explain.txt
    points-offset-explain.txt
  products/
    products-baseline/
      measurement.json
      k6-summary.json
      k6-exit-status.txt
      run-window.json
      pg-stat-statements.txt
  orders/
    orders-baseline/
      measurement.json
      k6-summary.json
      k6-exit-status.txt
      run-window.json
      pg-stat-statements.txt
  points/
    points-page0/
      measurement.json
      k6-summary.json
      k6-exit-status.txt
      run-window.json
      pg-stat-statements.txt
    points-page500/
      measurement.json
      k6-summary.json
      k6-exit-status.txt
      run-window.json
      pg-stat-statements.txt
  grafana-screenshots/
    products-baseline.png
    orders-baseline.png
    points-page0.png
    points-page500.png
```

## Task 1: Evidence Tooling 상태 확인 및 보강

**Files:**
- Verify: `scripts/run-k6-evidence.mjs`
- Verify: `scripts/run-k6-evidence.test.mjs`
- Modify if needed: `scripts/run-k6-evidence.mjs`
- Modify if needed: `scripts/run-k6-evidence.test.mjs`
- Verify: `.agents/rules/phase-evidence-collection.md`

- [ ] **Step 1: 현재 measurement 생성 상태 확인**

Run:

```bash
rtk grep -n "measurement" scripts/run-k6-evidence.mjs scripts/run-k6-evidence.test.mjs
```

Expected:

- `scripts/run-k6-evidence.mjs`에 `measurement.json` 생성 로직이 있다.
- `buildEvidencePaths()`가 `measurementFile`을 반환한다.
- k6 실행 종료 후 `measurement.json`을 생성한다.
- 이 조건을 만족하면 Step 2는 코드 수정 없이 확인만 하고 넘어간다.

- [ ] **Step 2: `scripts/run-k6-evidence.mjs` measurement contract 확인 또는 보강**

확인 또는 보강 요구사항:

- `buildEvidencePaths()`가 `measurementFile: "${evidenceDir}/measurement.json"`을 반환한다.
- k6 실행 종료 후 `measurement.json`을 생성한다.
- `exitStatus=0`은 `passed`, `exitStatus=99`는 `threshold_failed`, 그 외는 `execution_failed`로 기록한다.
- `k6-summary.json`, `run-window.json`, `k6-exit-status.txt` 경로를 evidence 파일 목록으로 기록한다.
- workload는 `k6/presets/<preset>.json`에서 읽는다.
- target은 scenario별로 고정한다.

Scenario별 target:

| scenario | method | endpoint | query params |
|---|---|---|---|
| `orders` | `GET` | `/api/orders` | `userId=1..1000`, `strategy=lazy` |
| `products` | `GET` | `/api/products` | `categoryId=1..200`, `status=ON_SALE/SOLD_OUT/DISCONTINUED`, `strategy=baseline` |
| `points` + `phase1-points-page0` | `GET` | `/api/points` | `userId=1..1000`, `page=0`, `size=20` |
| `points` + `phase1-points-page500` | `GET` | `/api/points` | `userId=1..1000`, `page=500`, `size=20` |

Guardrail:

- `points-page0` condition은 `phase1-points-page0` preset과만 함께 사용한다.
- `points-page500` condition은 `phase1-points-page500` preset과만 함께 사용한다.
- condition 이름만 page0/page500이고 preset이 `baseline`이면 evidence 디렉토리명과 실제 k6 요청 조건이 달라지므로 실패 또는 명시적 경고로 처리한다.

- [ ] **Step 3: measurement 테스트 추가**

Run:

```bash
rtk node --test scripts/run-k6-evidence.test.mjs
```

Expected:

- `measurementFile` 경로가 `docs/evidence/phase-01/orders/orders-baseline/measurement.json` 형태로 생성된다.
- `buildK6RunEnv()`는 `K6_LOG_FILE`을 넘기지 않는다.
- k6 exit 99도 `threshold_failed`로 manifest에 기록된다.
- points condition과 preset이 맞지 않으면 테스트가 실패하거나 경고 케이스가 검증된다.

## Task 2: Phase 1 SQL Evidence 파일 추가

**Files:**
- Create: `scripts/phase-01/00-phase1-db-state.sql`
- Create: `scripts/phase-01/01-reset-statistics.sql`
- Create: `scripts/phase-01/10-products-baseline-explain.sql`
- Create: `scripts/phase-01/20-orders-lazy-explain.sql`
- Create: `scripts/phase-01/30-points-offset-explain.sql`
- Create: `scripts/phase-01/40-pg-stat-statements.sql`

- [ ] **Step 1: `scripts/phase-01/00-phase1-db-state.sql` 추가**

Purpose:

- Phase 1 대상 테이블의 행 수, 인덱스, 제약조건, 데이터 분포를 evidence로 남긴다.

Required SQL blocks:

```sql
\pset pager off

\echo 'PHASE_01_DB_STATE_CAPTURE'
SELECT CURRENT_TIMESTAMP AS captured_at,
       current_database() AS database_name,
       current_user AS database_user;

\echo 'PHASE_01_TARGET_ROW_COUNTS'
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL SELECT 'product_sku', COUNT(*) FROM product_sku
UNION ALL SELECT 'product_image', COUNT(*) FROM product_image
ORDER BY table_name;

\echo 'PHASE_01_TARGET_INDEXES'
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('users', 'product', 'orders', 'order_item', 'point_history', 'product_sku', 'product_image')
ORDER BY tablename, indexname;

\echo 'PHASE_01_TARGET_CONSTRAINTS'
SELECT conrelid::regclass AS table_name,
       conname AS constraint_name,
       contype AS constraint_type,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
  AND conrelid::regclass::text IN ('users', 'product', 'orders', 'order_item', 'point_history', 'product_sku', 'product_image')
ORDER BY conrelid::regclass::text, contype, conname;

\echo 'PHASE_01_PRODUCT_CATEGORY_STATUS_DISTRIBUTION'
SELECT category_id, status, COUNT(*) AS product_count
FROM product
GROUP BY category_id, status
ORDER BY category_id, status;

\echo 'PHASE_01_ORDER_USER_COUNT_SUMMARY'
WITH user_order_counts AS (
  SELECT user_id, COUNT(*) AS order_count
  FROM orders
  GROUP BY user_id
)
SELECT COUNT(*) AS users_with_orders,
       MIN(order_count) AS min_orders_per_user,
       MAX(order_count) AS max_orders_per_user,
       ROUND(AVG(order_count), 2) AS avg_orders_per_user,
       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY order_count) AS median_orders_per_user
FROM user_order_counts;

\echo 'PHASE_01_TOP_ORDER_USERS'
SELECT user_id, COUNT(*) AS order_count
FROM orders
GROUP BY user_id
ORDER BY order_count DESC, user_id
LIMIT 20;

\echo 'PHASE_01_POINT_HISTORY_USER_COUNT_SUMMARY'
WITH user_point_counts AS (
  SELECT user_id, COUNT(*) AS point_count
  FROM point_history
  GROUP BY user_id
)
SELECT COUNT(*) AS users_with_points,
       MIN(point_count) AS min_points_per_user,
       MAX(point_count) AS max_points_per_user,
       ROUND(AVG(point_count), 2) AS avg_points_per_user,
       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY point_count) AS median_points_per_user
FROM user_point_counts;

\echo 'PHASE_01_TOP_POINT_USERS'
SELECT user_id, COUNT(*) AS point_count
FROM point_history
GROUP BY user_id
ORDER BY point_count DESC, user_id
LIMIT 20;
```

- [ ] **Step 2: `scripts/phase-01/01-reset-statistics.sql` 추가**

```sql
\pset pager off
\echo 'PHASE_01_RESET_PG_STAT_STATEMENTS'
SELECT pg_stat_statements_reset();
```

- [ ] **Step 3: `scripts/phase-01/10-products-baseline-explain.sql` 추가**

```sql
\pset pager off

\echo 'PRODUCTS_BASELINE_EXPLAIN'
EXPLAIN
SELECT *
FROM product
WHERE category_id = 1
  AND status = 'ON_SALE';

\echo 'PRODUCTS_BASELINE_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM product
WHERE category_id = 1
  AND status = 'ON_SALE';
```

- [ ] **Step 4: `scripts/phase-01/20-orders-lazy-explain.sql` 추가**

```sql
\pset pager off
\set order_user_id 950

\echo 'ORDERS_BY_USER_EXPLAIN'
EXPLAIN
SELECT *
FROM orders
WHERE user_id = :order_user_id
ORDER BY id ASC;

\echo 'ORDERS_BY_USER_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM orders
WHERE user_id = :order_user_id
ORDER BY id ASC;

\echo 'ORDER_ITEM_BY_ORDER_ID_EXPLAIN'
EXPLAIN
SELECT *
FROM order_item
WHERE order_id = (
  SELECT id
  FROM orders
  WHERE user_id = :order_user_id
  ORDER BY id ASC
  LIMIT 1
);

\echo 'ORDER_ITEM_BY_ORDER_ID_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM order_item
WHERE order_id = (
  SELECT id
  FROM orders
  WHERE user_id = :order_user_id
  ORDER BY id ASC
  LIMIT 1
);
```

- [ ] **Step 5: `scripts/phase-01/30-points-offset-explain.sql` 추가**

```sql
\pset pager off
\set point_user_id 5714
\set page_size 20
\set page500_offset 10000

\echo 'POINTS_PAGE0_EXPLAIN'
EXPLAIN
SELECT *
FROM point_history
WHERE user_id = :point_user_id
ORDER BY created_at DESC, id DESC
LIMIT :page_size OFFSET 0;

\echo 'POINTS_PAGE0_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :point_user_id
ORDER BY created_at DESC, id DESC
LIMIT :page_size OFFSET 0;

\echo 'POINTS_PAGE500_EXPLAIN'
EXPLAIN
SELECT *
FROM point_history
WHERE user_id = :point_user_id
ORDER BY created_at DESC, id DESC
LIMIT :page_size OFFSET :page500_offset;

\echo 'POINTS_PAGE500_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = :point_user_id
ORDER BY created_at DESC, id DESC
LIMIT :page_size OFFSET :page500_offset;

\echo 'POINTS_COUNT_EXPLAIN'
EXPLAIN
SELECT COUNT(*)
FROM point_history
WHERE user_id = :point_user_id;

\echo 'POINTS_COUNT_EXPLAIN_ANALYZE'
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*)
FROM point_history
WHERE user_id = :point_user_id;
```

- [ ] **Step 6: `scripts/phase-01/40-pg-stat-statements.sql` 추가**

```sql
\pset pager off

\echo 'PHASE_01_PG_STAT_STATEMENTS_TOP_20'
SELECT calls,
       ROUND(mean_exec_time::numeric, 2) AS mean_ms,
       ROUND(total_exec_time::numeric, 2) AS total_ms,
       rows,
       LEFT(query, 240) AS query
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

- [ ] **Step 7: SQL wrapper 테스트**

Run:

```bash
rtk node --test scripts/run-phase-sql.test.mjs
```

Expected:

- `run-phase-sql` 테스트가 통과한다.

## Task 3: 실행 전 도구 검증

**Files:**
- Verify only

- [ ] **Step 1: k6/evidence/grafana 관련 테스트 실행**

Run:

```bash
rtk node --test scripts/run-k6-evidence.test.mjs scripts/grafana-capture-utils.test.mjs scripts/makefile-evidence-targets.test.mjs
rtk bash scripts/test-k6-run.sh
rtk node scripts/verify-observability.test.mjs
rtk make grafana-generate
```

Expected:

- Node test는 fail 없이 종료한다.
- `scripts/test-k6-run.sh`가 종료 코드 0으로 끝난다.
- `scripts/verify-observability.test.mjs`가 `Observability configuration verified.`를 출력한다.
- `docker/grafana/dashboards/db-lab-overview.json`이 최신 dashboard 설정으로 생성된다.

## Task 4: Fresh DB와 Seed Baseline 준비

**Files:**
- Output: `docs/evidence/common/seed-loadtest/seed-state.txt`
- Output: `docs/evidence/phase-01/db/before-measurement-state.txt`

- [ ] **Step 1: fresh volume으로 인프라 시작**

Run:

```bash
rtk docker compose down -v
rtk docker compose up -d postgres prometheus grafana
rtk docker compose ps
```

Expected:

- `postgres`, `prometheus`, `grafana`가 running 상태다.

- [ ] **Step 2: loadtest seed 삽입과 공통 seed-state 저장**

Run:

```bash
rtk make seed-state SEED_PRESET=loadtest
```

Expected:

- seed가 완료된다.
- `docs/evidence/common/seed-loadtest/seed-state.txt`가 생성된다.
- 이 단계는 7~8분 이상 걸릴 수 있다.

- [ ] **Step 3: Phase 1 대상 DB 상태 저장**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/00-phase1-db-state.sql OUTPUT=docs/evidence/phase-01/db/before-measurement-state.txt
```

Expected:

- `before-measurement-state.txt`에 대상 테이블 행 수, 인덱스, 제약조건, 분포가 저장된다.
- 최소 확인값은 `users`, `product`, `orders`, `order_item`, `point_history`의 행 수다.

## Task 5: Spring 서버 실행과 API Smoke 저장

**Files:**
- Output: `docs/evidence/phase-01/api-smoke/*.json`

- [ ] **Step 1: Spring 서버 실행**

새 터미널에서 실행:

```bash
rtk bash scripts/server.sh pool10
```

Expected:

- Spring Boot가 `pool10` profile로 실행된다.
- `http://localhost:8080/actuator/health`가 응답한다.

- [ ] **Step 2: evidence 디렉토리 생성**

Run:

```bash
mkdir -p docs/evidence/phase-01/db docs/evidence/phase-01/api-smoke docs/evidence/phase-01/explain docs/evidence/phase-01/grafana-screenshots
```

- [ ] **Step 3: API smoke 저장**

Run:

```bash
curl -fsS --max-time 5 -o docs/evidence/phase-01/api-smoke/spring-health.json -w "health http_status=%{http_code} time_total=%{time_total}s\n" "http://localhost:8080/actuator/health"
curl -fsS --max-time 5 -o docs/evidence/phase-01/api-smoke/products-baseline.json -w "products http_status=%{http_code} time_total=%{time_total}s\n" "http://localhost:8080/api/products?categoryId=1&status=ON_SALE&strategy=baseline"
curl -fsS --max-time 5 -o docs/evidence/phase-01/api-smoke/orders-lazy-user950.json -w "orders http_status=%{http_code} time_total=%{time_total}s\n" "http://localhost:8080/api/orders?userId=950&strategy=lazy"
curl -fsS --max-time 5 -o docs/evidence/phase-01/api-smoke/points-page0-user5714.json -w "points-page0 http_status=%{http_code} time_total=%{time_total}s\n" "http://localhost:8080/api/points?userId=5714&page=0&size=20"
curl -fsS --max-time 5 -o docs/evidence/phase-01/api-smoke/points-page500-user5714.json -w "points-page500 http_status=%{http_code} time_total=%{time_total}s\n" "http://localhost:8080/api/points?userId=5714&page=500&size=20"
```

Expected:

- `spring-health.json`은 `UP` 상태를 포함한다.
- API smoke 파일은 HTTP 500 에러 JSON이 아니어야 한다.
- 각 curl은 HTTP 2xx가 아니거나 5초 안에 응답하지 않으면 실패한다.
- 0 byte 응답, connection refused, timeout이 발생하면 k6로 넘어가지 않고 Spring 서버 또는 DB 상태를 먼저 복구한다.

## Task 6: SQL 실행계획 Evidence 저장

**Files:**
- Output: `docs/evidence/phase-01/explain/products-baseline-explain.txt`
- Output: `docs/evidence/phase-01/explain/orders-lazy-explain.txt`
- Output: `docs/evidence/phase-01/explain/points-offset-explain.txt`

- [ ] **Step 1: products 실행계획 저장**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/10-products-baseline-explain.sql OUTPUT=docs/evidence/phase-01/explain/products-baseline-explain.txt
```

- [ ] **Step 2: orders 실행계획 저장**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/20-orders-lazy-explain.sql OUTPUT=docs/evidence/phase-01/explain/orders-lazy-explain.txt
```

- [ ] **Step 3: points 실행계획 저장**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/30-points-offset-explain.sql OUTPUT=docs/evidence/phase-01/explain/points-offset-explain.txt
```

Expected:

- 각 파일에 `EXPLAIN`과 `EXPLAIN ANALYZE` 결과가 둘 다 있다.
- `products`는 `product` 필터 쿼리의 scan 방식과 actual time을 보여준다.
- `orders`는 `orders.user_id` 조회와 `order_item.order_id` 조회의 scan 방식과 actual time을 보여준다.
- `points`는 page0, page500, count query의 scan 방식과 actual time을 보여준다.

## Task 7: k6, Grafana, pg_stat_statements Evidence 수집

**Files:**
- Output: `docs/evidence/phase-01/<scenario>/<condition>/{measurement.json,k6-summary.json,k6-exit-status.txt,run-window.json,pg-stat-statements.txt}`
- Output: `docs/evidence/phase-01/grafana-screenshots/*.png`

각 케이스는 아래 순서를 지킨다.

1. Spring health check
2. DB active app session 확인
3. 필요하면 Spring 재시작 또는 cooldown
4. `pg_stat_statements` reset
5. k6 evidence + Grafana capture 실행
6. `pg_stat_statements` snapshot 저장
7. k6 summary sanity check
8. 다음 케이스로 넘어가기

각 케이스 실행 전 공통 gate:

```bash
curl -fsS --max-time 3 -o /tmp/phase1-health.json -w "health http_status=%{http_code} time_total=%{time_total}s\n" http://localhost:8080/actuator/health
docker compose exec -T postgres psql -U app -d ecommerce -v ON_ERROR_STOP=1 -c "
SELECT state, COUNT(*) AS session_count
FROM pg_stat_activity
WHERE datname = 'ecommerce'
  AND usename = 'app'
GROUP BY state
ORDER BY state;
"
```

Gate 기준:

- health check가 3초 안에 HTTP 2xx를 반환해야 한다.
- 이전 k6의 app active session이 계속 남아 있으면 다음 케이스로 넘어가지 않는다.
- orders/products처럼 앱을 포화시킬 수 있는 케이스 뒤에는 Spring을 재시작하거나, active session이 정리될 때까지 cooldown 후 다시 health check를 통과시킨다.
- `pg_stat_statements` reset은 health와 DB session gate를 통과한 뒤 실행한다.

- [ ] **Step 1: products baseline**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/01-reset-statistics.sql
STRATEGY=baseline rtk npm run evidence:capture -- --phase phase-01 --scenario products --preset baseline --pool pool10 --condition products-baseline --table product --output docs/evidence/phase-01/grafana-screenshots/products-baseline.png
rtk make phase-sql FILE=scripts/phase-01/40-pg-stat-statements.sql OUTPUT=docs/evidence/phase-01/products/products-baseline/pg-stat-statements.txt
```

Expected:

- `docs/evidence/phase-01/products/products-baseline/k6-summary.json` 생성
- `docs/evidence/phase-01/products/products-baseline/run-window.json` 생성
- `docs/evidence/phase-01/products/products-baseline/k6-exit-status.txt` 생성
- `docs/evidence/phase-01/products/products-baseline/measurement.json` 생성
- `docs/evidence/phase-01/grafana-screenshots/products-baseline.png` 생성
- `pg-stat-statements.txt`에 product 조회 쿼리가 포함됨
- `k6-summary.json`의 `data_received`가 0보다 크고, `http_req_failed`가 1.0이 아니어야 한다.

- [ ] **Step 2: orders baseline**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/01-reset-statistics.sql
STRATEGY=lazy rtk npm run evidence:capture -- --phase phase-01 --scenario orders --preset baseline --pool pool10 --condition orders-baseline --table 'orders|order_item|product_sku|product_image' --output docs/evidence/phase-01/grafana-screenshots/orders-baseline.png
rtk make phase-sql FILE=scripts/phase-01/40-pg-stat-statements.sql OUTPUT=docs/evidence/phase-01/orders/orders-baseline/pg-stat-statements.txt
```

Expected:

- `docs/evidence/phase-01/orders/orders-baseline/k6-summary.json` 생성
- `docs/evidence/phase-01/orders/orders-baseline/run-window.json` 생성
- `docs/evidence/phase-01/orders/orders-baseline/k6-exit-status.txt` 생성
- `docs/evidence/phase-01/orders/orders-baseline/measurement.json` 생성
- `docs/evidence/phase-01/grafana-screenshots/orders-baseline.png` 생성
- `pg-stat-statements.txt`에 `order_item` 반복 조회가 포함됨
- `k6-summary.json`의 `data_received`가 0보다 크다.
- `http_req_failed=1.0`이고 `data_received=0`이면 성능 evidence가 아니라 앱 포화/응답 실패 evidence로 분류하고 재실험한다.

- [ ] **Step 3: points page0**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/01-reset-statistics.sql
rtk npm run evidence:capture -- --phase phase-01 --scenario points --preset phase1-points-page0 --pool pool10 --condition points-page0 --table point_history --output docs/evidence/phase-01/grafana-screenshots/points-page0.png
rtk make phase-sql FILE=scripts/phase-01/40-pg-stat-statements.sql OUTPUT=docs/evidence/phase-01/points/points-page0/pg-stat-statements.txt
```

Expected:

- `docs/evidence/phase-01/points/points-page0/k6-summary.json` 생성
- `docs/evidence/phase-01/points/points-page0/run-window.json` 생성
- `docs/evidence/phase-01/points/points-page0/k6-exit-status.txt` 생성
- `docs/evidence/phase-01/points/points-page0/measurement.json` 생성
- `docs/evidence/phase-01/grafana-screenshots/points-page0.png` 생성
- `pg-stat-statements.txt`에 point_history page query와 count query가 포함됨
- `measurement.json`의 `preset`은 `phase1-points-page0`이고 `target.queryParams.page`는 `0`이어야 한다.
- `k6-summary.json`의 `data_received`가 0보다 크다.
- `http_req_failed=1.0`이고 `data_received=0`이면 성능 evidence가 아니라 앱 포화/응답 실패 evidence로 분류하고 재실험한다.

- [ ] **Step 4: points page500**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/01-reset-statistics.sql
rtk npm run evidence:capture -- --phase phase-01 --scenario points --preset phase1-points-page500 --pool pool10 --condition points-page500 --table point_history --output docs/evidence/phase-01/grafana-screenshots/points-page500.png
rtk make phase-sql FILE=scripts/phase-01/40-pg-stat-statements.sql OUTPUT=docs/evidence/phase-01/points/points-page500/pg-stat-statements.txt
```

Expected:

- `docs/evidence/phase-01/points/points-page500/k6-summary.json` 생성
- `docs/evidence/phase-01/points/points-page500/run-window.json` 생성
- `docs/evidence/phase-01/points/points-page500/k6-exit-status.txt` 생성
- `docs/evidence/phase-01/points/points-page500/measurement.json` 생성
- `docs/evidence/phase-01/grafana-screenshots/points-page500.png` 생성
- `pg-stat-statements.txt`에 point_history offset query와 count query가 포함됨
- `measurement.json`의 `preset`은 `phase1-points-page500`이고 `target.queryParams.page`는 `500`이어야 한다.
- `k6-summary.json`의 `data_received`가 0보다 크다.
- `http_req_failed=1.0`이고 `data_received=0`이면 성능 evidence가 아니라 앱 포화/응답 실패 evidence로 분류하고 재실험한다.

## Task 8: 측정 후 DB 상태 저장

**Files:**
- Output: `docs/evidence/phase-01/db/after-measurement-state.txt`

- [ ] **Step 1: 측정 후 DB 상태 저장**

Run:

```bash
rtk make phase-sql FILE=scripts/phase-01/00-phase1-db-state.sql OUTPUT=docs/evidence/phase-01/db/after-measurement-state.txt
```

Expected:

- row count와 인덱스 상태가 측정 전 상태와 의미 있게 다르지 않다.
- k6 실행이 Phase 1 대상 테이블에 데이터를 추가/삭제하지 않았음을 확인할 수 있다.

## Task 9: Evidence Index 작성

**Files:**
- Create or Modify: `docs/evidence/phase-01/README.md`

- [ ] **Step 1: evidence README 작성**

`docs/evidence/phase-01/README.md`에는 아래 내용을 포함한다.

- Phase 1에서 확인하려던 내용
- DB 상태 evidence 링크
- API smoke 링크
- SQL 실행계획 링크
- 케이스별 k6 evidence 링크
- 케이스별 `pg_stat_statements` 링크
- Grafana screenshot 링크
- k6 exit code 99가 있으면 `threshold_failed`로 기록하되, `http_req_failed=1.0`이고 `data_received=0`인 run은 성능 evidence가 아니라 앱 포화/응답 실패 evidence로 분류한다.

필수로 링크할 파일:

```text
docs/evidence/phase-01/db/before-measurement-state.txt
docs/evidence/phase-01/db/after-measurement-state.txt
docs/evidence/phase-01/explain/products-baseline-explain.txt
docs/evidence/phase-01/explain/orders-lazy-explain.txt
docs/evidence/phase-01/explain/points-offset-explain.txt
docs/evidence/phase-01/products/products-baseline/k6-summary.json
docs/evidence/phase-01/orders/orders-baseline/k6-summary.json
docs/evidence/phase-01/points/points-page0/k6-summary.json
docs/evidence/phase-01/points/points-page500/k6-summary.json
```

## Task 10: 작성 전 결과 타당성 판단

**Files:**
- Read: `docs/roadmap/02-phase-1-baseline.md`
- Read: `docs/phases/01-baseline/scope.md`
- Read: `.agents/rules/phase-report-writing.md`
- Read: `docs/evidence/phase-01/README.md`
- Read: evidence files referenced by README

- [ ] **Step 1: 필수 파일 존재 확인**

Run:

```bash
test -f docs/evidence/phase-01/products/products-baseline/measurement.json
test -f docs/evidence/phase-01/orders/orders-baseline/measurement.json
test -f docs/evidence/phase-01/points/points-page0/measurement.json
test -f docs/evidence/phase-01/points/points-page500/measurement.json
test -f docs/evidence/phase-01/products/products-baseline/k6-summary.json
test -f docs/evidence/phase-01/orders/orders-baseline/k6-summary.json
test -f docs/evidence/phase-01/points/points-page0/k6-summary.json
test -f docs/evidence/phase-01/points/points-page500/k6-summary.json
test -f docs/evidence/phase-01/db/before-measurement-state.txt
test -f docs/evidence/phase-01/explain/products-baseline-explain.txt
test -f docs/evidence/phase-01/explain/orders-lazy-explain.txt
test -f docs/evidence/phase-01/explain/points-offset-explain.txt
```

Expected:

- 모든 command가 exit 0이다.

- [ ] **Step 2: 결과 타당성 판단**

판단 기준:

- 케이스별 실행 조건이 `measurement.json`에 구분되어 있다.
- `k6-summary.json`에 requests, rate, failed rate, p95, p99, dropped iterations가 있다.
- `k6-exit-status.txt`의 99는 threshold 실패로 해석한다.
- exit 99라도 `data_received > 0`이고 실제 HTTP 응답이 수집된 run은 baseline 한계를 보여주는 성능 evidence로 유지할 수 있다.
- exit 99이면서 `http_req_failed=1.0`이고 `data_received=0`이면 timeout/connection refused/0 byte 응답 run으로 보고 성능 비교 evidence에서 제외한다.
- 위 응답 실패 run은 앱 포화/운영 실패 evidence로 별도 기록하거나, report 작성 전에 해당 케이스를 재실험한다.
- `run-window.json`이 Grafana screenshot의 시간 범위와 연결된다.
- DB 상태 evidence에 row count, index, constraint가 있다.
- SQL 실행계획 evidence에 `EXPLAIN`과 `EXPLAIN ANALYZE`가 둘 다 있다.
- `pg_stat_statements`가 k6 결과와 같은 방향을 가리킨다.

판단 결과:

- `작성 가능`: 바로 Task 11로 진행
- `한계 명시 후 작성 가능`: 한계를 report 회고에 먼저 적고 Task 11로 진행
- `재실험 필요`: 사용자에게 어떤 케이스의 어떤 수치가 왜 문제인지 보고하고 Task 7 또는 Task 6으로 되돌아감

## Task 11: Phase 1 report.md 작성

**Files:**
- Modify: `docs/phases/01-baseline/report.md`
- Modify if needed: `docs/phases/01-baseline/README.md`

- [ ] **Step 1: report 구조를 먼저 잡기**

고정 제목은 아니지만 아래 흐름을 지킨다.

1. Phase 1에서 확인하려던 내용
2. 핵심 결론과 주요 수치
3. 데이터와 테이블 상태
4. 케이스별 측정 조건
5. 케이스별 결과 수치
6. SQL 실행계획과 내부 쿼리 동작
7. 결과가 나온 이유
8. 다음 Phase에서 비교할 기준
9. 회고와 보완사항
10. Grafana 대시보드와 주요 근거 링크

- [ ] **Step 2: report 본문 작성**

작성 규칙:

- 한국어로 작성한다.
- 추상적인 표현을 쓰지 않는다.
- 모든 핵심 주장은 수치와 파일 링크를 붙인다.
- k6 수치는 `k6-summary.json`에서 가져온다.
- SQL 수치는 `EXPLAIN ANALYZE`와 `pg_stat_statements`에서 가져온다.
- Grafana는 시계열 보조 근거로 사용한다.
- 모르는 내용은 추정하지 않는다.
- 이상하거나 애매한 결과는 회고와 보완사항에 둔다.

- [ ] **Step 3: stale HTML 처리**

현재 `docs/phases/01-baseline/README.md`는 `report.html`을 링크한다. `report.html` 생성 절차가 없으면 둘 중 하나를 선택한다.

- `report.html`을 갱신할 수 있는 절차를 확인하고 새 report 기준으로 다시 생성한다.
- 생성 절차가 없으면 README에서 `report.html` 링크를 제거해 stale 문서 링크를 남기지 않는다.

## Task 12: Phase review skill로 검증하고 report 보정

**Files:**
- Read: `docs/phases/01-baseline/report.md`
- Read: `docs/evidence/phase-01/README.md`
- Modify if needed: `docs/phases/01-baseline/report.md`
- Modify if needed: `docs/evidence/phase-01/README.md`

- [ ] **Step 1: `phase-evidence-portfolio-review`로 검토**

검토 요청:

```text
phase-evidence-portfolio-review 기준으로 Phase 1을 검토한다.
대상 Phase 문서: docs/phases/01-baseline/
Evidence: docs/evidence/phase-01/
Roadmap: docs/roadmap/02-phase-1-baseline.md
```

Expected:

- 구현 완료 여부
- Evidence 충분성
- Claim -> Evidence 매트릭스
- report.md 독립성
- 지표 이상 징후
- 다음 Phase 준비도
- 포트폴리오 관점 보완점

- [ ] **Step 2: 검토 결과 반영**

반영 규칙:

- P0는 반드시 수정한다.
- P1은 report의 근거 링크, 수치, 한계 설명에 반영한다.
- P2는 문서 가독성이나 포트폴리오 표현을 개선할 때만 반영한다.

## Task 13: 최종 검증

**Files:**
- Verify: `docs/phases/01-baseline/report.md`
- Verify: `docs/evidence/phase-01/README.md`
- Verify: `docs/evidence/phase-01/**`

- [ ] **Step 1: 필수 evidence 파일 최종 확인**

Run:

```bash
find docs/evidence/phase-01 -maxdepth 4 -type f | sort
```

Expected:

- 각 k6 케이스에 `measurement.json`, `k6-summary.json`, `k6-exit-status.txt`, `run-window.json`, `pg-stat-statements.txt`가 있다.
- `explain/` 아래 3개 실행계획 파일이 있다.
- `db/` 아래 측정 전/후 상태 파일이 있다.
- `grafana-screenshots/` 아래 4개 screenshot이 있다.

- [ ] **Step 2: 금지 문자열 확인**

Run:

```bash
rtk grep -n "TODO\\|TBD\\|대충\\|느린 것 같다\\|좋아진 것 같다\\|k6-summary.txt" docs/phases/01-baseline/report.md docs/evidence/phase-01/README.md
```

Expected:

- match가 없거나, historical evidence 설명처럼 의도된 문맥만 남아 있다.

- [ ] **Step 3: markdown 링크 점검**

Run:

```bash
rtk grep -n "\\](.*docs/evidence\\|\\](../../evidence" docs/phases/01-baseline/report.md docs/evidence/phase-01/README.md
```

Expected:

- report에서 참조한 evidence 링크가 실제 파일과 맞는다.

## 완료 기준

- `docs/evidence/phase-01/README.md`가 새 evidence를 인덱싱한다.
- `docs/phases/01-baseline/report.md`가 새 evidence 기반으로 작성되어 있다.
- report는 처음 읽는 사람이 Phase 1의 목적, 데이터 상태, 테스트 조건, 결과 수치, 원인 해석, 한계, 다음 Phase 기준을 이해할 수 있다.
- `phase-evidence-portfolio-review` 검토에서 `ready` 또는 `ready after fixes` 상태이고, `ready after fixes`라면 필요한 수정이 반영되어 있다.
