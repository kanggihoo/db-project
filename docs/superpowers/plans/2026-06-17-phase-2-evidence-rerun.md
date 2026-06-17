# Phase 2 Evidence Rerun Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Phase 2 인덱스 실험을 새로 실행해 `docs/evidence/phase-02` 아래에 재현 가능한 evidence 구조를 만들고, `docs/phases/02-indexes`의 `README.md`, `scope.md`, `runbook.md`, `observability.md`, `report.md`를 새 evidence 기준으로 갱신한다.

**Architecture:** 기존 실행 파일과 설정을 재사용한다. SQL evidence는 `scripts/phase-02/*.sql`과 `make phase-sql`로 생성하고, k6 evidence는 `make evidence-capture` 또는 `npm run evidence:capture` wrapper로 생성해 `measurement.json`, `k6-summary.json`, `run-window.json`, `k6-exit-status.txt` 계약을 지킨다. 최종 `report.md`는 원본 evidence 파일에서 숫자를 다시 읽어 작성하며, 기존 과거 evidence의 숫자는 사용하지 않는다.

**Tech Stack:** PostgreSQL 17, Spring Boot, Docker Compose, k6, Prometheus remote write, Grafana, Node.js evidence wrapper, Markdown.

---

## File Structure

### Evidence Output Structure

새 실행의 산출물은 아래 구조를 목표로 한다.

```text
docs/evidence/phase-02/
  README.md
  db-state/
    00-seed-loadtest-state.txt
    10-pre-index-product-state.txt
    20-post-index-product-state.txt
  products/
    pre-index/
      explain.txt
    pool10-post-index/
      explain.txt
      measurement.json
      k6-summary.json
      k6-exit-status.txt
      run-window.json
      pg-stat-statements.txt
  grafana-screenshots/
    products-post-index.png
  sql-only/
    single-status-index.txt
    composite-order-index.txt
    covering-index.txt
    partial-index.txt
```

각 파일의 책임은 다음과 같다.

| Path | 책임 |
|---|---|
| `docs/evidence/phase-02/README.md` | Phase 2 evidence index. 새 evidence 구조와 재생성 명령 링크를 요약한다. |
| `docs/evidence/phase-02/db-state/00-seed-loadtest-state.txt` | `loadtest` seed 직후 공통 데이터 상태를 Phase 2 실행 시점에 확인한 snapshot. |
| `docs/evidence/phase-02/db-state/10-pre-index-product-state.txt` | Phase 2 main index 적용 전 `product` row count, index, 분포 상태 snapshot. |
| `docs/evidence/phase-02/db-state/20-post-index-product-state.txt` | `idx_product_category_status` 적용 후 `product` index 상태 snapshot. |
| `docs/evidence/phase-02/products/pre-index/explain.txt` | main product query의 pre-index 실행계획 evidence. |
| `docs/evidence/phase-02/products/pool10-post-index/explain.txt` | main product query의 post-index 실행계획 evidence. |
| `docs/evidence/phase-02/products/pool10-post-index/measurement.json` | k6 실행 조건 manifest. |
| `docs/evidence/phase-02/products/pool10-post-index/k6-summary.json` | k6 원본 summary. 최종 API p95, p99, failure, dropped iterations의 기준 원본. |
| `docs/evidence/phase-02/products/pool10-post-index/k6-exit-status.txt` | k6 exit code. |
| `docs/evidence/phase-02/products/pool10-post-index/run-window.json` | Grafana capture 시간 범위의 기준. |
| `docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt` | post-index k6 실행 직후 product query 누적 DB 실행시간 snapshot. |
| `docs/evidence/phase-02/grafana-screenshots/products-post-index.png` | 같은 run-window 기준 Grafana screenshot. |
| `docs/evidence/phase-02/sql-only/*.txt` | SQL-only 보조 실험 evidence. API/k6 latency와 직접 비교하지 않는다. |

### Phase Documentation Files

수정 대상 문서는 아래 5개로 제한한다.

| Path | 수정 책임 |
|---|---|
| `docs/phases/02-indexes/README.md` | 현재 상태를 "재수집 예정" 또는 "완료"로 정확히 반영하고 evidence index 링크를 유지한다. |
| `docs/phases/02-indexes/scope.md` | 완료 조건을 새 evidence 계약 기준으로 조정한다. |
| `docs/phases/02-indexes/runbook.md` | raw `k6/run.sh` 중심 절차를 wrapper 중심 절차로 교체하고 DB state, EXPLAIN, k6, Grafana, SQL-only 명령을 모두 명시한다. |
| `docs/phases/02-indexes/observability.md` | report에서 어떤 원본 파일의 어떤 metric을 읽을지 기준을 명확히 한다. |
| `docs/phases/02-indexes/report.md` | 새 evidence 수집 후 원본 수치로 최종 결과 보고서를 재작성한다. 수집 전에는 기존 수치를 결론으로 유지하지 않는다. |

### Existing Execution Files To Reuse

아래 파일은 새로 만들지 않고 재사용한다.

| Path | 사용 목적 |
|---|---|
| `scripts/seed.sh` | `./scripts/seed.sh loadtest`로 기준 데이터 생성. |
| `scripts/server.sh` | `./scripts/server.sh pool10`으로 Spring 서버 실행. |
| `scripts/db-state/00-seed-state.sql` | seed 기준 상태와 Phase 2 DB 상태 snapshot 생성. |
| `scripts/phase-02/00-clean-product-indexes.sql` | Phase 2 product 실험 index cleanup. |
| `scripts/phase-02/01-main-pre-index-explain.sql` | pre-index main query 실행계획 생성. |
| `scripts/phase-02/02-create-main-index.sql` | `idx_product_category_status` 생성. |
| `scripts/phase-02/03-main-post-index-explain.sql` | post-index main query 실행계획 생성. |
| `scripts/phase-02/04-product-pg-stat-statements.sql` | post-index product query `pg_stat_statements` snapshot 생성. |
| `scripts/phase-02/10-single-status-index.sql` | SQL-only 단일 컬럼 인덱스 실험. |
| `scripts/phase-02/20-composite-order-index.sql` | SQL-only 복합 인덱스 순서 실험. |
| `scripts/phase-02/30-covering-index.sql` | SQL-only covering index 실험. |
| `scripts/phase-02/40-partial-index.sql` | SQL-only partial index 실험. |
| `k6/products-test.js` | Phase 2 product API workload. |
| `k6/presets/baseline.json` | 50 rps, 5m, `categoryId=1..200` workload preset. |
| `scripts/run-k6-evidence.mjs` | k6 evidence wrapper. |
| `scripts/capture-grafana-dashboard.mjs` | Grafana screenshot capture. |

---

### Task 1: Evidence Directory Contract 문서화

**Files:**
- Modify: `docs/evidence/phase-02/README.md`

- [ ] **Step 1: 현재 README를 새 수집 구조 기준으로 교체한다**

`docs/evidence/phase-02/README.md`를 아래 구조로 갱신한다.

```markdown
# Phase 2 Evidence

이 디렉토리는 Phase 2 상품 검색 API의 인덱스 적용 전후 비교 Evidence와 SQL-only 보조 실험 Evidence를 저장한다.

## Measurement Condition

| 항목 | 값 |
|---|---|
| phase | `phase-02` |
| scenario | `products` |
| preset | `baseline` |
| condition | `pool10-post-index` |
| pool | `pool10` |
| seed | `loadtest` |
| API | `GET /api/products?categoryId=&status=&strategy=baseline` |
| main index | `idx_product_category_status ON product(category_id, status)` |

## Expected Structure

| Evidence | Path |
|---|---|
| Seed/loadtest DB state | [db-state/00-seed-loadtest-state.txt](./db-state/00-seed-loadtest-state.txt) |
| Pre-index product state | [db-state/10-pre-index-product-state.txt](./db-state/10-pre-index-product-state.txt) |
| Post-index product state | [db-state/20-post-index-product-state.txt](./db-state/20-post-index-product-state.txt) |
| Pre-index EXPLAIN | [products/pre-index/explain.txt](./products/pre-index/explain.txt) |
| Post-index EXPLAIN | [products/pool10-post-index/explain.txt](./products/pool10-post-index/explain.txt) |
| k6 measurement manifest | [products/pool10-post-index/measurement.json](./products/pool10-post-index/measurement.json) |
| k6 summary | [products/pool10-post-index/k6-summary.json](./products/pool10-post-index/k6-summary.json) |
| k6 exit status | [products/pool10-post-index/k6-exit-status.txt](./products/pool10-post-index/k6-exit-status.txt) |
| k6 run window | [products/pool10-post-index/run-window.json](./products/pool10-post-index/run-window.json) |
| Post-index pg_stat_statements | [products/pool10-post-index/pg-stat-statements.txt](./products/pool10-post-index/pg-stat-statements.txt) |
| Post-index Grafana screenshot | [grafana-screenshots/products-post-index.png](./grafana-screenshots/products-post-index.png) |

## SQL-only Auxiliary Experiments

| Experiment | Path |
|---|---|
| Single-column index selectivity | [sql-only/single-status-index.txt](./sql-only/single-status-index.txt) |
| Composite index order | [sql-only/composite-order-index.txt](./sql-only/composite-order-index.txt) |
| Covering index | [sql-only/covering-index.txt](./sql-only/covering-index.txt) |
| Partial index | [sql-only/partial-index.txt](./sql-only/partial-index.txt) |

## Notes

- k6 성능 수치는 `k6-summary.json`을 기준 원본으로 사용한다.
- Grafana screenshot은 `run-window.json`의 시간 범위에 맞춰 캡처한다.
- SQL-only evidence는 인덱스 planner 동작 설명용이며 API latency와 직접 비교하지 않는다.
- 모든 파일은 `docs/phases/02-indexes/runbook.md`의 명령으로 재생성할 수 있어야 한다.
```

- [ ] **Step 2: 링크 대상 디렉토리를 미리 만든다**

```bash
mkdir -p \
  docs/evidence/phase-02/db-state \
  docs/evidence/phase-02/products/pre-index \
  docs/evidence/phase-02/products/pool10-post-index \
  docs/evidence/phase-02/grafana-screenshots \
  docs/evidence/phase-02/sql-only
```

- [ ] **Step 3: README 링크 검증을 실행한다**

```bash
node --test scripts/*.test.mjs
```

Expected: `# pass 36` and exit `0`.

---

### Task 2: Phase Scope를 새 Evidence 계약 기준으로 갱신

**Files:**
- Modify: `docs/phases/02-indexes/scope.md`

- [ ] **Step 1: 목표 문단에 evidence manifest 조건을 추가한다**

`## 목표` 아래 bullet 목록을 아래처럼 갱신한다.

```markdown
- Phase 1 `products/pool10-baseline`과 비교 가능한 pre/post Evidence를 수집한다.
- 인덱스 적용 전 상품 검색 SQL의 `EXPLAIN`과 `EXPLAIN (ANALYZE, BUFFERS)`를 기록한다.
- `idx_product_category_status ON product(category_id, status)`를 적용한다.
- 인덱스 적용 후 `EXPLAIN`, k6, `pg_stat_statements`, Grafana Evidence를 수집한다.
- k6 실행은 `measurement.json`, `k6-summary.json`, `k6-exit-status.txt`, `run-window.json`을 포함해야 한다.
- 단일 컬럼 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스는 SQL-only 보조 실험으로 분리해 기록한다.
```

- [ ] **Step 2: 대상 API 표를 strategy까지 명확히 한다**

`대상 API와 테이블` 표를 아래처럼 갱신한다.

```markdown
| 항목 | 값 |
|---|---|
| API | `GET /api/products?categoryId=&status=&strategy=baseline` |
| Repository | `ProductRepository.findByCategoryIdAndStatus` |
| Table | `product` |
| Main query shape | `product where category_id = ? and status = ?` |
| Main index | `idx_product_category_status ON product(category_id, status)` |
| k6 scenario | `products` |
| k6 preset | `baseline` |
| k6 condition | `pool10-post-index` |
| Spring profile | `pool10` |
| Seed preset | `loadtest` |
```

- [ ] **Step 3: 완료 조건을 evidence 파일 기준으로 갱신한다**

`## 완료 조건`을 아래로 교체한다.

```markdown
## 완료 조건

- [ ] Phase 1의 `products/pool10-baseline` 결과를 비교 기준으로 사용했다.
- [ ] `loadtest` seed와 `pool10` Spring profile을 측정 조건으로 고정했다.
- [ ] 측정 전 `product` table의 row count, index 목록, 상태 분포를 evidence로 저장했다.
- [ ] 상품 검색 대표 SQL의 인덱스 적용 전 `EXPLAIN`과 `EXPLAIN (ANALYZE, BUFFERS)` 결과를 기록했다.
- [ ] `idx_product_category_status` 적용 후 실행계획과 실행시간 변화를 기록했다.
- [ ] 같은 `loadtest`, `pool10`, `products baseline`, `strategy=baseline` 조건에서 k6를 실행했다.
- [ ] k6 evidence run에 `measurement.json`, `k6-summary.json`, `k6-exit-status.txt`, `run-window.json`이 포함됐다.
- [ ] `pg_stat_statements.mean_exec_time`, k6 p95/p99/failure/dropped iterations, Grafana 지표를 Phase 1 결과와 비교했다.
- [ ] 단일 인덱스 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스는 SQL-only 보조 실험으로 분리해 해석했다.
- [ ] 남아 있는 병목이 쿼리 수 문제인지 확인하고 Phase 3으로 넘길 근거를 기록했다.
```

---

### Task 3: Runbook을 실제 재수집 절차로 갱신

**Files:**
- Modify: `docs/phases/02-indexes/runbook.md`

- [ ] **Step 1: 사전 조건을 Docker daemon 확인까지 포함해 갱신한다**

`## 사전 조건`을 아래로 교체한다.

```markdown
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
```

- [ ] **Step 2: DB 상태 확인 절차를 추가한다**

`## 메인 API 비교 실험` 앞에 아래 섹션을 추가한다.

```markdown
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
```

- [ ] **Step 3: EXPLAIN 실행 명령을 wrapper 중심으로 갱신한다**

pre-index EXPLAIN 명령을 아래로 교체한다.

```bash
make phase-sql \
  FILE=scripts/phase-02/01-main-pre-index-explain.sql \
  OUTPUT=docs/evidence/phase-02/products/pre-index/explain.txt
```

post-index 준비와 EXPLAIN 명령을 아래로 교체한다.

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

- [ ] **Step 4: k6 실행 명령을 evidence wrapper로 교체한다**

raw `PHASE=phase-02 POOL=pool10 ./k6/run.sh ... | tee ...` 명령을 제거하고 아래를 넣는다.

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

이 명령은 아래 파일을 생성해야 한다.

```text
docs/evidence/phase-02/products/pool10-post-index/measurement.json
docs/evidence/phase-02/products/pool10-post-index/k6-summary.json
docs/evidence/phase-02/products/pool10-post-index/k6-exit-status.txt
docs/evidence/phase-02/products/pool10-post-index/run-window.json
docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

- [ ] **Step 5: pg_stat_statements 수집 명령을 wrapper로 갱신한다**

```bash
make phase-sql \
  FILE=scripts/phase-02/04-product-pg-stat-statements.sql \
  OUTPUT=docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

- [ ] **Step 6: SQL-only 보조 실험 명령을 wrapper로 갱신한다**

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

- [ ] **Step 7: evidence 파일 검증 명령을 추가한다**

runbook 마지막에 아래 섹션을 추가한다.

```markdown
## Evidence 파일 검증

```bash
test -f docs/evidence/phase-02/products/pool10-post-index/measurement.json
test -f docs/evidence/phase-02/products/pool10-post-index/k6-summary.json
test -f docs/evidence/phase-02/products/pool10-post-index/k6-exit-status.txt
test -f docs/evidence/phase-02/products/pool10-post-index/run-window.json
test -f docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
test -f docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

```bash
node -e "const fs=require('fs'); const m=JSON.parse(fs.readFileSync('docs/evidence/phase-02/products/pool10-post-index/measurement.json','utf8')); if (m.phase !== 'phase-02' || m.scenario !== 'products' || m.preset !== 'baseline' || m.pool !== 'pool10' || m.condition !== 'pool10-post-index') process.exit(1); console.log('phase-02 measurement ok');"
```
```

---

### Task 4: Observability 문서를 원본 파일 기준으로 갱신

**Files:**
- Modify: `docs/phases/02-indexes/observability.md`

- [ ] **Step 1: k6 지표 원본 파일을 명시한다**

`## k6 지표` 섹션 첫 문단에 아래 문장을 추가한다.

```markdown
최종 report의 k6 수치는 `docs/evidence/phase-02/products/pool10-post-index/k6-summary.json`을 기준 원본으로 사용한다. Grafana와 Prometheus는 실행 중 시간 흐름을 설명하는 보조 evidence로만 사용한다.
```

- [ ] **Step 2: k6 metric 표를 확장한다**

기존 k6 표를 아래로 교체한다.

```markdown
| Metric | 원본 | 해석 |
|---|---|---|
| `http_reqs.count` | `k6-summary.json` | 완료된 HTTP 요청 수다. Phase 1 요청 수와 비교한다. |
| `http_req_duration.p(95)` | `k6-summary.json` | API 요청 95%가 완료된 시간이다. Phase 1 p95와 비교한다. |
| `http_req_duration.p(99)` | `k6-summary.json` | tail latency 확인용 보조 지표다. |
| `http_req_failed.value` | `k6-summary.json` | timeout, 5xx, expected response 실패율이다. |
| `dropped_iterations.count` | `k6-summary.json` | 목표 arrival rate를 맞추지 못해 시작하지 못한 iteration 수다. |
| exit code | `k6-exit-status.txt` | `0`은 passed, `99`는 threshold_failed, 그 외는 execution_failed로 본다. |
```

- [ ] **Step 3: Grafana 확인 기준에 run-window를 추가한다**

`## Grafana 확인 행` 앞에 아래 문장을 추가한다.

```markdown
Grafana screenshot은 `docs/evidence/phase-02/products/pool10-post-index/run-window.json`의 `grafanaFrom`, `grafanaTo` 범위와 같은 시간대를 사용한다.
```

---

### Task 5: Phase README 상태를 재수집 흐름에 맞게 정리

**Files:**
- Modify: `docs/phases/02-indexes/README.md`

- [ ] **Step 1: 현재 상태 문단을 재수집 기준으로 갱신한다**

실제 evidence 재수집 전에는 `## 현재 상태`를 아래로 교체한다.

```markdown
## 현재 상태

Phase 2는 새 evidence 수집을 진행할 예정이다.

- 실행 파일과 설정은 준비되어 있다.
- 새 수집은 `loadtest`, `pool10`, `products baseline`, `strategy=baseline`, `condition=pool10-post-index` 조건으로 진행한다.
- k6 evidence는 wrapper를 사용해 `measurement.json`, `k6-summary.json`, `k6-exit-status.txt`, `run-window.json`을 함께 남긴다.
- 최종 완료 판정과 `report.md`의 수치는 새로 수집한 원본 evidence를 기준으로 작성한다.
```

실제 evidence 수집과 report 작성이 끝난 뒤에는 같은 섹션을 아래로 교체한다.

```markdown
## 현재 상태

Phase 2는 완료된 상태다.

- 메인 상품 검색 쿼리의 pre-index/post-index 실행계획 기록 완료
- `idx_product_category_status` 적용 후 k6, `pg_stat_statements`, Grafana evidence 저장 완료
- k6 evidence manifest와 run window 저장 완료
- SQL-only 보조 실험으로 단일/복합/커버링/부분 인덱스 동작 확인 완료
- Phase 3 N+1 최적화로 넘길 근거 기록 완료
```

---

### Task 6: Evidence 수집 실행

**Files:**
- Generate: `docs/evidence/phase-02/db-state/*.txt`
- Generate: `docs/evidence/phase-02/products/**/*.txt`
- Generate: `docs/evidence/phase-02/products/pool10-post-index/*.json`
- Generate: `docs/evidence/phase-02/grafana-screenshots/products-post-index.png`
- Generate: `docs/evidence/phase-02/sql-only/*.txt`

- [ ] **Step 1: Docker daemon과 compose 설정을 확인한다**

```bash
docker info
docker compose config --quiet
```

Expected: both exit `0`.

- [ ] **Step 2: 인프라를 기동한다**

```bash
docker compose up -d postgres postgres_exporter prometheus grafana
docker compose ps
```

Expected: `postgres`, `postgres_exporter`, `prometheus`, `grafana` are running or healthy.

- [ ] **Step 3: loadtest seed를 실행한다**

```bash
./scripts/seed.sh loadtest
```

Expected: Gradle bootRun exits `0`.

- [ ] **Step 4: Spring 서버를 pool10으로 실행한다**

별도 터미널에서 실행한다.

```bash
./scripts/server.sh pool10
```

서버 기동 후 다른 터미널에서 확인한다.

```bash
curl -fsS http://localhost:8080/actuator/health
```

Expected: response contains `"status":"UP"`.

- [ ] **Step 5: seed DB state를 저장한다**

```bash
make phase-sql \
  FILE=scripts/db-state/00-seed-state.sql \
  OUTPUT=docs/evidence/phase-02/db-state/00-seed-loadtest-state.txt
```

Expected: output file contains `product` row count and product status distribution.

- [ ] **Step 6: pre-index 상태와 실행계획을 저장한다**

```bash
make phase-sql FILE=scripts/phase-02/00-clean-product-indexes.sql

docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
docker compose exec -T postgres psql -U app -d ecommerce -c "VACUUM ANALYZE product;"

make phase-sql \
  FILE=scripts/db-state/00-seed-state.sql \
  OUTPUT=docs/evidence/phase-02/db-state/10-pre-index-product-state.txt

make phase-sql \
  FILE=scripts/phase-02/01-main-pre-index-explain.sql \
  OUTPUT=docs/evidence/phase-02/products/pre-index/explain.txt
```

Expected: `products/pre-index/explain.txt` contains the representative pre-index plan.

- [ ] **Step 7: main index를 적용하고 post-index 상태와 실행계획을 저장한다**

```bash
make phase-sql FILE=scripts/phase-02/02-create-main-index.sql

docker compose exec -T postgres psql -U app -d ecommerce -c "VACUUM ANALYZE product;"

make phase-sql \
  FILE=scripts/db-state/00-seed-state.sql \
  OUTPUT=docs/evidence/phase-02/db-state/20-post-index-product-state.txt

make phase-sql \
  FILE=scripts/phase-02/03-main-post-index-explain.sql \
  OUTPUT=docs/evidence/phase-02/products/pool10-post-index/explain.txt
```

Expected: `20-post-index-product-state.txt` contains `idx_product_category_status`; post-index explain references index-based access if PostgreSQL planner selects it.

- [ ] **Step 8: k6와 Grafana evidence를 수집한다**

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

Expected: wrapper creates `measurement.json`, `k6-summary.json`, `k6-exit-status.txt`, `run-window.json`, and `products-post-index.png`.

- [ ] **Step 9: post-index pg_stat_statements를 저장한다**

```bash
make phase-sql \
  FILE=scripts/phase-02/04-product-pg-stat-statements.sql \
  OUTPUT=docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

Expected: output includes product query row with `calls`, `mean_ms`, `total_ms`, `rows`.

- [ ] **Step 10: SQL-only 보조 실험을 실행한다**

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

Expected: each output file contains before/after `EXPLAIN` output for the SQL-only topic.

- [ ] **Step 11: evidence 파일 존재와 measurement 계약을 검증한다**

```bash
test -f docs/evidence/phase-02/products/pool10-post-index/measurement.json
test -f docs/evidence/phase-02/products/pool10-post-index/k6-summary.json
test -f docs/evidence/phase-02/products/pool10-post-index/k6-exit-status.txt
test -f docs/evidence/phase-02/products/pool10-post-index/run-window.json
test -f docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
test -f docs/evidence/phase-02/grafana-screenshots/products-post-index.png
```

```bash
node -e "const fs=require('fs'); const m=JSON.parse(fs.readFileSync('docs/evidence/phase-02/products/pool10-post-index/measurement.json','utf8')); const expected={phase:'phase-02',scenario:'products',preset:'baseline',pool:'pool10',condition:'pool10-post-index'}; for (const [k,v] of Object.entries(expected)) { if (m[k] !== v) { console.error(k, m[k], '!=', v); process.exit(1); } } if (m.target.endpoint !== '/api/products') process.exit(1); if (m.target.queryParams.strategy !== 'baseline') process.exit(1); console.log('phase-02 measurement ok');"
```

Expected: `phase-02 measurement ok`.

---

### Task 7: Report 작성 전 수치 추출

**Files:**
- Read: `docs/evidence/phase-02/products/pool10-post-index/k6-summary.json`
- Read: `docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt`
- Read: `docs/evidence/phase-02/products/pre-index/explain.txt`
- Read: `docs/evidence/phase-02/products/pool10-post-index/explain.txt`
- Read: `docs/evidence/phase-02/db-state/*.txt`
- Read: `docs/evidence/phase-02/sql-only/*.txt`

- [ ] **Step 1: k6 수치를 추출한다**

```bash
node - <<'NODE'
const fs = require('fs');
const summary = JSON.parse(fs.readFileSync('docs/evidence/phase-02/products/pool10-post-index/k6-summary.json', 'utf8'));
const metrics = summary.metrics;
const out = {
  httpReqs: metrics.http_reqs?.count,
  requestRate: metrics.http_reqs?.rate,
  p95: metrics.http_req_duration?.['p(95)'],
  p99: metrics.http_req_duration?.['p(99)'],
  failedRate: metrics.http_req_failed?.value,
  droppedIterations: metrics.dropped_iterations?.count ?? 0,
};
console.log(JSON.stringify(out, null, 2));
NODE
```

Expected: printed JSON has `httpReqs`, `p95`, `p99`, `failedRate`, `droppedIterations`.

- [ ] **Step 2: k6 exit status를 확인한다**

```bash
cat docs/evidence/phase-02/products/pool10-post-index/k6-exit-status.txt
```

Expected: `0` or `99`. If any other value appears, do not write final conclusions; investigate execution failure first.

- [ ] **Step 3: `pg_stat_statements` 수치를 읽는다**

```bash
cat docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```

Record `calls`, `mean_ms`, `total_ms`, `rows` for the product query.

- [ ] **Step 4: 실행계획 핵심 값을 읽는다**

```bash
cat docs/evidence/phase-02/products/pre-index/explain.txt
cat docs/evidence/phase-02/products/pool10-post-index/explain.txt
```

Record scan type, actual rows, rows removed by filter if present, buffers, planning time, execution time.

- [ ] **Step 5: SQL-only 핵심 관찰을 읽는다**

```bash
cat docs/evidence/phase-02/sql-only/single-status-index.txt
cat docs/evidence/phase-02/sql-only/composite-order-index.txt
cat docs/evidence/phase-02/sql-only/covering-index.txt
cat docs/evidence/phase-02/sql-only/partial-index.txt
```

Record only planner behavior and execution-plan evidence. Do not compare these values directly with API latency.

---

### Task 8: Final Report 재작성

**Files:**
- Modify: `docs/phases/02-indexes/report.md`

- [ ] **Step 1: 작성 가능 여부를 판정한다**

아래 조건을 모두 만족하면 `작성 가능`으로 본다.

```text
- measurement.json이 phase-02/products/baseline/pool10/pool10-post-index 조건을 가진다.
- k6-summary.json에서 request count, p95, p99, failed rate, dropped iterations를 읽을 수 있다.
- k6-exit-status.txt가 0 또는 99다.
- pre-index/post-index explain 파일이 있고 실행계획과 execution time을 읽을 수 있다.
- post-index pg_stat_statements 파일에서 product query의 calls, mean_ms, total_ms, rows를 읽을 수 있다.
- db-state 파일에서 product row count와 index 상태를 확인할 수 있다.
```

조건이 부족하면 `report.md`를 최종 결론으로 작성하지 말고, 부족한 evidence를 사용자에게 보고한다.

- [ ] **Step 2: report.md 구조를 아래 순서로 작성한다**

```markdown
# Phase 2 결과 보고서

> 목적: Phase 1 상품 검색 Baseline과 Phase 2 post-index 측정값을 비교하고, SQL-only 보조 실험으로 인덱스 설계 원리를 정리한다.

## 결론

## 측정 조건

## 데이터와 테이블 상태

## Phase 1 대비 Phase 2 비교

## 실행계획 비교

## post-index pg_stat_statements

## SQL-only 보조 실험 결과

## 해석

## Phase 3 Handoff

## Evidence Links
```

- [ ] **Step 3: 결론은 새 evidence 숫자로만 작성한다**

결론 문단은 아래 형식을 사용하되, 숫자는 Task 7에서 추출한 값으로 채운다.

```markdown
Phase 2는 기존 `GET /api/products?categoryId=&status=&strategy=baseline` API 흐름을 바꾸지 않고 PostgreSQL 인덱스만 적용했다. 메인 인덱스는 `idx_product_category_status ON product(category_id, status)`이며, 대표 product 필터 쿼리의 실행계획은 `[pre plan]`에서 `[post plan]`으로 바뀌었다.

같은 `loadtest`, `pool10`, `products baseline`, `strategy=baseline` 조건에서 API p95는 Phase 1 `[phase1 p95]`에서 Phase 2 `[phase2 p95]`로 바뀌었다. `pg_stat_statements` 기준 SQL 평균 실행시간은 Phase 1 `[phase1 mean]`에서 Phase 2 `[phase2 mean]`으로 바뀌었다.
```

- [ ] **Step 4: Phase 1 대비 표를 새 수치로 작성한다**

```markdown
| Metric | Phase 1 Baseline | Phase 2 Post-index | 변화 |
|---|---:|---:|---:|
| API p95 | 17.24ms | [phase2-p95]ms | [delta] |
| API p99 | [phase1-p99 if available] | [phase2-p99]ms | [delta or 보조 지표] |
| SQL mean time | 7.90ms | [phase2-sql-mean]ms | [delta] |
| SQL total time | 118,489.64ms | [phase2-sql-total]ms | [delta] |
| requests | 15,001 | [phase2-http-reqs] | [delta] |
| failed | 0.00% | [phase2-failed-rate]% | [delta] |
| dropped iterations | 0 | [phase2-dropped] | [delta] |
```

If Phase 1 p99 is not available from the baseline evidence, write `기준 evidence 없음` instead of inventing a value.

- [ ] **Step 5: Evidence Links를 핵심 파일만 연결한다**

```markdown
## Evidence Links

- [Seed/loadtest DB state](../../evidence/phase-02/db-state/00-seed-loadtest-state.txt)
- [Pre-index product state](../../evidence/phase-02/db-state/10-pre-index-product-state.txt)
- [Post-index product state](../../evidence/phase-02/db-state/20-post-index-product-state.txt)
- [Pre-index EXPLAIN](../../evidence/phase-02/products/pre-index/explain.txt)
- [Post-index EXPLAIN](../../evidence/phase-02/products/pool10-post-index/explain.txt)
- [k6 measurement](../../evidence/phase-02/products/pool10-post-index/measurement.json)
- [k6 summary](../../evidence/phase-02/products/pool10-post-index/k6-summary.json)
- [k6 run window](../../evidence/phase-02/products/pool10-post-index/run-window.json)
- [post-index pg_stat_statements](../../evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt)
- [Grafana screenshot](../../evidence/phase-02/grafana-screenshots/products-post-index.png)
- [SQL-only evidence index](../../evidence/phase-02/README.md)
```

---

### Task 9: Final Verification

**Files:**
- Verify: `docs/evidence/phase-02/**`
- Verify: `docs/phases/02-indexes/*.md`

- [ ] **Step 1: 스크립트 테스트를 실행한다**

```bash
node --test scripts/*.test.mjs
bash scripts/test-k6-run.sh
```

Expected: both exit `0`.

- [ ] **Step 2: Phase 2 핵심 evidence 파일을 검증한다**

```bash
for file in \
  docs/evidence/phase-02/db-state/00-seed-loadtest-state.txt \
  docs/evidence/phase-02/db-state/10-pre-index-product-state.txt \
  docs/evidence/phase-02/db-state/20-post-index-product-state.txt \
  docs/evidence/phase-02/products/pre-index/explain.txt \
  docs/evidence/phase-02/products/pool10-post-index/explain.txt \
  docs/evidence/phase-02/products/pool10-post-index/measurement.json \
  docs/evidence/phase-02/products/pool10-post-index/k6-summary.json \
  docs/evidence/phase-02/products/pool10-post-index/k6-exit-status.txt \
  docs/evidence/phase-02/products/pool10-post-index/run-window.json \
  docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt \
  docs/evidence/phase-02/grafana-screenshots/products-post-index.png \
  docs/evidence/phase-02/sql-only/single-status-index.txt \
  docs/evidence/phase-02/sql-only/composite-order-index.txt \
  docs/evidence/phase-02/sql-only/covering-index.txt \
  docs/evidence/phase-02/sql-only/partial-index.txt
do
  test -s "$file" || { echo "missing or empty: $file"; exit 1; }
done
```

Expected: no output and exit `0`.

- [ ] **Step 3: measurement contract를 검증한다**

```bash
node -e "const fs=require('fs'); const m=JSON.parse(fs.readFileSync('docs/evidence/phase-02/products/pool10-post-index/measurement.json','utf8')); const required=['phase','scenario','condition','preset','pool','target','workload','evidence','execution']; for (const key of required) { if (!(key in m)) { console.error('missing', key); process.exit(1); } } const checks=[['phase','phase-02'],['scenario','products'],['condition','pool10-post-index'],['preset','baseline'],['pool','pool10']]; for (const [key,value] of checks) { if (m[key] !== value) { console.error(key, m[key], '!=', value); process.exit(1); } } if (m.target.endpoint !== '/api/products') process.exit(1); if (m.target.queryParams.strategy !== 'baseline') process.exit(1); if (m.workload.rate !== 50) process.exit(1); if (m.workload.duration !== '5m') process.exit(1); console.log('measurement contract ok');"
```

Expected: `measurement contract ok`.

- [ ] **Step 4: report가 원본 evidence 링크를 참조하는지 확인한다**

```bash
grep -q "k6-summary.json" docs/phases/02-indexes/report.md
grep -q "pg-stat-statements.txt" docs/phases/02-indexes/report.md
grep -q "run-window.json" docs/phases/02-indexes/report.md
grep -q "products-post-index.png" docs/phases/02-indexes/report.md
```

Expected: all commands exit `0`.

- [ ] **Step 5: 변경사항을 검토한다**

```bash
git diff -- docs/evidence/phase-02 docs/phases/02-indexes
```

Expected: diff only includes Phase 2 evidence and Phase 2 documentation changes.

---

## Self-Review

- Spec coverage: 이 계획은 evidence directory 구조, `docs/phases/02-indexes` 5개 문서 수정, k6/SQL/Grafana evidence 수집, 최종 `report.md` 작성 게이트를 포함한다.
- Placeholder scan: 실행 명령, 파일 경로, expected output을 명시했다. 수치가 실제 실행 후에만 결정되는 부분은 Task 7에서 원본 파일로 추출하도록 고정했다.
- Type consistency: `phase=phase-02`, `scenario=products`, `preset=baseline`, `pool=pool10`, `condition=pool10-post-index`, `strategy=baseline`을 전 task에서 동일하게 사용한다.
