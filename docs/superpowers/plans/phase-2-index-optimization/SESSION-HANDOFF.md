# Phase 2 Session Handoff

> 작성 시점: 2026-05-19
> 대상 브랜치: `codex/phase-2-index-optimization`

## 현재 상태

Phase 2 Index Optimization 작업은 001, 002, 003의 메인 product index evidence 수집까지 완료했다.

현재 브랜치는 `origin/codex/phase-2-index-optimization`보다 4커밋 앞서 있다.

```text
ffb69b1 docs: update phase 2 main experiment report
cea9360 docs: capture phase 2 main product index evidence
1c9f6a6 docs: add phase 2 index experiment scripts
a8dc670 docs: scaffold phase 2 index documentation
```

## 완료된 작업

### 001 Phase Documentation Scaffold

- `docs/phases/02-indexes/` 표준 문서 5개 생성
- `docs/evidence/phase-02/README.md` 생성
- `docs/superpowers/README.md`의 Phase 2 plan 링크 확인

### 002 SQL Script Kit

- `scripts/phase-02/` SQL 스크립트 9개 생성
- `docs/guides/scripts.md`에 Phase 2 SQL script kit 설명 추가

주요 스크립트:

- `00-clean-product-indexes.sql`
- `01-main-pre-index-explain.sql`
- `02-create-main-index.sql`
- `03-main-post-index-explain.sql`
- `04-product-pg-stat-statements.sql`
- `10-single-status-index.sql`
- `20-composite-order-index.sql`
- `30-covering-index.sql`
- `40-partial-index.sql`

### 003 Main Product Index Experiment

실제 PostgreSQL, Spring Boot `pool10`, k6 `products baseline` 조건으로 메인 실험 evidence를 생성했다.

생성 및 커밋된 evidence:

- `docs/evidence/phase-02/products/pre-index/explain.txt`
- `docs/evidence/phase-02/products/pool10-post-index/explain.txt`
- `docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt`
- `docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt`
- `docs/evidence/phase-02/README.md`의 Main API Comparison Notes

`docs/phases/02-indexes/report.md`도 메인 실험 결과로 갱신했다.

## 측정 결과 요약

### EXPLAIN

| Evidence | Plan | Execution Time | Buffers |
|---|---|---:|---|
| pre-index | `Seq Scan on product` | 13.869ms | `shared hit=3109` |
| post-index | `Bitmap Index Scan` + `Bitmap Heap Scan` | 1.584ms | `shared hit=366 read=2` |

post-index plan은 `Bitmap Index Scan on idx_product_category_status`를 사용했다.

### k6

조건:

- `phase=phase-02`
- `scenario=products`
- `preset=baseline`
- `pool=pool10`
- request rate: 50 rps

결과:

| Metric | Phase 1 baseline | Phase 2 post-index |
|---|---:|---:|
| API p95 | 17.24ms | 12.14ms |
| requests | 15,001 | 15,001 |
| failed | 0.00% | 0.00% |
| dropped iterations | 0 | 0 |

### pg_stat_statements

| Metric | Phase 1 baseline | Phase 2 post-index |
|---|---:|---:|
| calls | 15,001 | 15,001 |
| mean_exec_time | 7.90ms | 0.40ms |
| total_exec_time | 118,489.64ms | 5,986.88ms |
| rows | 2,508,501 | 2,476,309 |

## 남은 작업

### Grafana screenshot

사용자 지시로 screenshot은 나중에 다시 찍기로 했다.

현재 `docs/evidence/phase-02/grafana-screenshots/`가 untracked 상태로 남아 있을 수 있다. 이 파일은 임시 캡처이며, 다음 세션에서 다시 캡처하거나 삭제한 뒤 새 evidence로 저장한다.

권장:

1. Grafana `DB Lab Overview`를 연다.
2. 변수 설정:
   - `$phase=phase-02`
   - `$scenario=products`
   - `$preset=baseline`
   - `$pool=pool10`
   - `$table=product`
3. Run Summary와 Table Access 또는 Phase 2 Index Focus가 보이는 화면을 저장한다.
4. 파일 경로:
   - `docs/evidence/phase-02/grafana-screenshots/products-post-index.png`
5. `docs/evidence/phase-02/README.md` 링크가 실제 파일과 맞는지 확인한다.

### 004 SQL-only Auxiliary Experiments

다음 계획 파일:

- `docs/superpowers/plans/phase-2-index-optimization/004-sql-only-auxiliary-experiments.md`

실행할 SQL-only evidence:

- `scripts/phase-02/10-single-status-index.sql`
- `scripts/phase-02/20-composite-order-index.sql`
- `scripts/phase-02/30-covering-index.sql`
- `scripts/phase-02/40-partial-index.sql`

예상 evidence 경로:

- `docs/evidence/phase-02/sql-only/single-status-index.txt`
- `docs/evidence/phase-02/sql-only/composite-order-index.txt`
- `docs/evidence/phase-02/sql-only/covering-index.txt`
- `docs/evidence/phase-02/sql-only/partial-index.txt`

주의:

- SQL-only 결과는 API/k6 latency와 직접 비교하지 않는다.
- 각 SQL-only 스크립트는 실험 인덱스를 정리하고 자기 실험 인덱스만 생성한다.
- 결과 해석은 `docs/phases/02-indexes/report.md`의 SQL-only 보조 실험 결과 섹션에 채운다.

### 005 Report And Phase Closeout

004와 screenshot이 완료된 뒤 진행한다.

다음 계획 파일:

- `docs/superpowers/plans/phase-2-index-optimization/005-report-and-phase-closeout.md`

예상 작업:

- `docs/phases/02-indexes/report.md` 최종 갱신
- Phase 1 대비 Phase 2 결과 정리
- SQL-only 인덱스 개념 실험 해석 정리
- Phase 3 N+1 handoff 구체화

## 현재 working tree 주의사항

현재 `git status --short`에서 다음 untracked 항목이 남아 있을 수 있다.

```text
?? .tmp/
?? docs/evidence/phase-02/grafana-screenshots/
```

- `.tmp/`는 서버 로그, Chrome profile, 임시 Node CDP 패키지 등 작업 중 생긴 임시 파일이다. 커밋하지 않는다.
- `docs/evidence/phase-02/grafana-screenshots/`는 사용자가 나중에 screenshot을 다시 찍기로 한 항목이다. 다음 세션에서 실제 evidence로 확정하기 전까지 커밋하지 않는다.

## 실행 중일 수 있는 프로세스

다음 프로세스가 남아 있을 수 있다.

| PID | Process | Purpose |
|---:|---|---|
| 20284 | `java` | Spring Boot `pool10` server |
| 20396 | `chrome` | headless Chrome remote debugging for Grafana capture |

필요 없으면 다음처럼 정리한다.

```powershell
Stop-Process -Id 20284,20396
```

프로세스가 이미 종료됐는지 먼저 확인하려면:

```powershell
Get-Process -Id 20284,20396 -ErrorAction SilentlyContinue
```

## 다음 세션 시작 체크리스트

1. `git status --short --branch`로 브랜치와 untracked 항목을 확인한다.
2. 필요하면 Spring Boot/Chrome 임시 프로세스를 정리한다.
3. screenshot을 먼저 다시 찍을지, 004 SQL-only 실험을 먼저 진행할지 결정한다.
4. 004 진행 전 Docker/PostgreSQL 상태와 `product` row count를 확인한다.
5. SQL-only 결과를 저장한 뒤 `report.md`의 SQL-only 섹션을 갱신한다.

## 검증에 사용한 주요 명령

```bash
rtk grep "Phase 2 main product query: pre-index" docs/evidence/phase-02/products/pre-index/explain.txt
rtk grep "Seq Scan" docs/evidence/phase-02/products/pre-index/explain.txt
rtk grep "Phase 2 main product query: post-index" docs/evidence/phase-02/products/pool10-post-index/explain.txt
rtk grep "idx_product_category_status" docs/evidence/phase-02/products/pool10-post-index/explain.txt
rtk grep "http_req_duration" docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
rtk grep "http_req_failed" docs/evidence/phase-02/products/pool10-post-index/k6-summary.txt
rtk grep "from product" docs/evidence/phase-02/products/pool10-post-index/pg-stat-statements.txt
```
