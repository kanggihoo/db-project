# 001 Phase Documentation Scaffold

## Goal

Phase 7 표준 문서 5개와 evidence index 골격을 만든다.

## Files

- Create: `docs/phases/07-pagination/README.md`
- Create: `docs/phases/07-pagination/scope.md`
- Create: `docs/phases/07-pagination/runbook.md`
- Create: `docs/phases/07-pagination/observability.md`
- Create: `docs/phases/07-pagination/report.md`
- Create: `docs/evidence/phase-07/README.md`

## Steps

- [ ] **Step 1: Create Phase 7 README**

Create `docs/phases/07-pagination/README.md`:

```md
# Phase 7. 페이지네이션 최적화

Phase 7은 **Point History** 단일 테이블을 대상으로 Offset pagination의 deep page 비용과 Cursor pagination의 개선 효과를 측정한다.

## 상태

- 상태: planned
- 기준 spec: [2026-05-27-phase-7-pagination-design.md](../../superpowers/specs/2026-05-27-phase-7-pagination-design.md)
- roadmap: [08-phase-7-pagination.md](../../roadmap/08-phase-7-pagination.md)

## 주요 질문

- Offset 방식은 인덱스가 있어도 깊은 페이지에서 왜 느려지는가?
- Cursor 방식은 같은 위치의 다음 페이지를 어떻게 더 적은 skip 비용으로 조회하는가?
- `Page<T>`가 만드는 COUNT 쿼리는 목록 조회에서 어떤 추가 비용을 만드는가?

## 문서

| 문서 | 목적 |
|---|---|
| [scope.md](./scope.md) | 범위, 제외 범위, 완료 조건 |
| [runbook.md](./runbook.md) | 실행 절차 |
| [observability.md](./observability.md) | 관측 지표와 해석 |
| [report.md](./report.md) | 결과 기록 |

## Evidence

- [Phase 7 evidence index](../../evidence/phase-07/README.md)
```

- [ ] **Step 2: Create scope document**

Create `docs/phases/07-pagination/scope.md`:

```md
# Phase 7 Scope

## 목표

`point_history`에서 Offset deep page, Cursor pagination, COUNT 쿼리 비용을 비교한다.

## 포함 범위

- 전체 `point_history` SQL-only Offset/Cursor 실행계획 비교
- hot user의 **Point History** API/k6 비교
- `GET /api/points/cursor` 추가
- `Page` 방식 COUNT 쿼리와 Cursor/Slice 방식의 count 없는 조회 비교
- Phase 7 evidence를 `docs/evidence/phase-07/`에 기록

## 제외 범위

- **Delivery Tracking** 필수 실험
- seed preset 변경
- 사용자-facing 전체 `point_history` 목록 API
- 프론트엔드 또는 페이지 번호 UI
- PostgreSQL 외 RDBMS 비교

## 완료 조건

- [ ] 전체 `point_history` SQL-only 실험에서 Offset shallow/deep과 Cursor deep 실행계획을 비교했다.
- [ ] hot user 후보와 선정 user의 `point_count`, `midPage`, `deepPage`를 기록했다.
- [ ] Offset `page=0`, `midPage`, `deepPage` k6 p95를 비교했다.
- [ ] Cursor API k6 p95를 기록했다.
- [ ] `pg_stat_statements`로 Offset/Page count query와 Cursor API의 count query 제거 여부를 확인했다.
- [ ] 결과를 `report.md`와 `docs/evidence/phase-07/README.md`에 연결했다.
```

- [ ] **Step 3: Create runbook**

Create `docs/phases/07-pagination/runbook.md`:

````md
# Phase 7 Runbook

## 1. Seed

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

## 2. Data profile

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/00-data-profile.sql
```

저장 위치:

- `docs/evidence/phase-07/data-profile/point-history-total-count.txt`
- `docs/evidence/phase-07/data-profile/hot-user-point-counts.txt`
- `docs/evidence/phase-07/data-profile/selected-user-and-pages.txt`

## 3. SQL-only 실행계획

전체 테이블 실험:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/10-global-index-prepare.sql
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/11-global-offset-page0-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/12-global-offset-deep-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/13-global-cursor-deep-explain.sql
```

hot user 실험:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/20-user-index-prepare.sql
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=1 -v deep_offset=1400 -f /workspace/scripts/phase-07/21-user-offset-page0-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=1 -v deep_offset=1400 -f /workspace/scripts/phase-07/22-user-offset-deep-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=1 -v deep_offset=1400 -f /workspace/scripts/phase-07/23-user-cursor-deep-explain.sql
```

## 4. API tests

```bash
cd ecommerce
./gradlew test --tests "*PointHistoryRepositoryTest" --tests "*PointCursorApiTest"
./gradlew compileJava
```

## 5. k6

Offset:

```bash
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=page0 PRESET=presets/points-page0.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=mid PRESET=presets/points-mid.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=deep PRESET=presets/points-deep.json k6 run k6/points-test.js
```

Cursor:

```bash
PHASE=phase-07 SCENARIO=points-cursor PRESET_NAME=cursor PRESET=presets/points-cursor.json k6 run k6/points-cursor-test.js
```

## 6. pg_stat_statements

각 API run 직후 snapshot을 저장한다.

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/30-pg-stat-statements.sql
```
````

- [ ] **Step 4: Create observability document**

Create `docs/phases/07-pagination/observability.md`:

```md
# Phase 7 Observability

## 주요 증거

| 증거 | 역할 |
|---|---|
| `EXPLAIN (ANALYZE, BUFFERS)` | Offset deep과 Cursor deep의 DB 실행계획 비교 |
| k6 summary | API p95 비교 |
| `pg_stat_statements` | data query와 count query 호출/시간 비교 |
| Grafana screenshot | 같은 시간대의 HTTP, Hikari, table access 보조 확인 |

## Grafana 사용

별도 Phase 7 dashboard를 만들지 않고 shared `DB Lab Overview`를 사용한다.

캡처 대상:

- Run Summary
- k6 Load
- Hikari Pool
- Table Access에서 `point_history`

Grafana는 보조 evidence다. Offset이 읽고 지나간 row 수 차이는 `EXPLAIN (ANALYZE, BUFFERS)`로 판단한다.

## k6 label 원칙

- `phase`: `phase-07`
- `scenario`: `points-offset`, `points-cursor`
- `preset`: `page0`, `mid`, `deep`, `cursor`
- `pool`: `pool10`

`userId`, `page`, SQL text는 label로 넣지 않는다.
```

- [ ] **Step 5: Create report placeholder**

Create `docs/phases/07-pagination/report.md`:

```md
# Phase 7 Report

## Summary

아직 evidence를 수집하지 않았다. `005-evidence-capture-and-report.md` 실행 후 이 문서에 실제 값을 반영한다.

## Measurement Condition

- seed preset: `loadtest`
- table: `point_history`
- selected user: `005-evidence-capture-and-report.md`에서 선택한 user
- size: `20`
- midPage: `005-evidence-capture-and-report.md`에서 계산한 중간 페이지
- deepPage: `005-evidence-capture-and-report.md`에서 계산한 깊은 페이지

## SQL-only Result

| 실험 | evidence | 주요 결과 |
|---|---|---|
| global offset page0 | - | evidence 수집 전 |
| global offset deep | - | evidence 수집 전 |
| global cursor deep | - | evidence 수집 전 |

## API/k6 Result

| 실험 | p95 | evidence |
|---|---:|---|
| offset page0 | - | - |
| offset mid | - | - |
| offset deep | - | - |
| cursor | - | - |

## COUNT Query Result

| 조건 | count query 호출 | evidence |
|---|---:|---|
| Offset/Page | - | - |
| Cursor | - | - |

## Phase 8 Handoff

Phase 8에서는 HTTP p95, Hikari pending/active, `pg_stat_activity`, `pg_stat_statements`를 연결해 병목 관측 흐름을 정리한다.
```

- [ ] **Step 6: Create evidence index**

Create `docs/evidence/phase-07/README.md`:

```md
# Phase 7 Evidence

## Measurement Condition

- seed preset: `loadtest`
- table: `point_history`
- selected user: `005-evidence-capture-and-report.md`에서 선택한 user
- size: `20`

## Evidence Index

| 종류 | 파일 | 설명 |
|---|---|---|
| data profile | [data-profile/point-history-total-count.txt](./data-profile/point-history-total-count.txt) | 전체 `point_history` row 수 |
| data profile | [data-profile/hot-user-point-counts.txt](./data-profile/hot-user-point-counts.txt) | hot user 후보 |
| data profile | [data-profile/selected-user-and-pages.txt](./data-profile/selected-user-and-pages.txt) | 선정 user와 page 계산 |
| explain | [explain/global-offset-page0.txt](./explain/global-offset-page0.txt) | 전체 테이블 Offset shallow |
| explain | [explain/global-offset-deep.txt](./explain/global-offset-deep.txt) | 전체 테이블 Offset deep |
| explain | [explain/global-cursor-deep.txt](./explain/global-cursor-deep.txt) | 전체 테이블 Cursor deep |
| explain | [explain/user-offset-page0.txt](./explain/user-offset-page0.txt) | hot user Offset shallow |
| explain | [explain/user-offset-deep.txt](./explain/user-offset-deep.txt) | hot user Offset deep |
| explain | [explain/user-cursor-deep.txt](./explain/user-cursor-deep.txt) | hot user Cursor deep |
| k6 | [k6/offset-page0-summary.txt](./k6/offset-page0-summary.txt) | Offset page0 p95 |
| k6 | [k6/offset-mid-summary.txt](./k6/offset-mid-summary.txt) | Offset mid p95 |
| k6 | [k6/offset-deep-summary.txt](./k6/offset-deep-summary.txt) | Offset deep p95 |
| k6 | [k6/cursor-summary.txt](./k6/cursor-summary.txt) | Cursor p95 |
| pg_stat_statements | [pg-stat-statements/offset-page-api.txt](./pg-stat-statements/offset-page-api.txt) | Offset/Page SQL snapshot |
| pg_stat_statements | [pg-stat-statements/cursor-api.txt](./pg-stat-statements/cursor-api.txt) | Cursor SQL snapshot |
| grafana | [grafana/db-lab-overview-phase-7.png](./grafana/db-lab-overview-phase-7.png) | shared dashboard screenshot |
```

- [ ] **Step 7: Verify documents exist**

Run:

```bash
rtk powershell -NoProfile -Command "$paths = @('docs/phases/07-pagination/README.md','docs/phases/07-pagination/scope.md','docs/phases/07-pagination/runbook.md','docs/phases/07-pagination/observability.md','docs/phases/07-pagination/report.md','docs/evidence/phase-07/README.md'); $paths | ForEach-Object { if (-not (Test-Path $_)) { throw \"Missing $_\" } }"
```

Expected: command exits 0.

- [ ] **Step 8: Commit**

```bash
git add docs/phases/07-pagination docs/evidence/phase-07/README.md
git commit -m "docs(phase7): scaffold pagination phase docs"
```
