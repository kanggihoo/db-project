# Phase 7 Pagination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `point_history`에서 Offset deep page 비용, Cursor 개선 효과, `Page<T>` COUNT 쿼리 비용을 측정하고 Phase 7 evidence로 남긴다.

**Architecture:** Phase 7은 `point_history` 단일 테이블을 대상으로 SQL-only 원리 확인과 hot user API/k6 실험을 분리한다. 기존 `/api/points`는 Offset/Page baseline으로 유지하고 `/api/points/cursor`를 새로 추가한다. 실험 인덱스와 EXPLAIN 쿼리는 `scripts/phase-07/`에 두고, 증거는 `docs/evidence/phase-07/`에 기록한다.

**Tech Stack:** Spring Boot 4, Spring Data JPA, Java records, PostgreSQL, `psql`, `EXPLAIN (ANALYZE, BUFFERS)`, `pg_stat_statements`, k6, Grafana, JUnit Jupiter, AssertJ, Testcontainers.

---

## Spec

구현 기준 문서:

- [Phase 7 페이지네이션 최적화 설계](../../specs/2026-05-27-phase-7-pagination-design.md)
- [Phase 7 roadmap](../../../roadmap/08-phase-7-pagination.md)

## Plan Decisions

| 질문 | 결정 |
|---|---|
| 대상 테이블 | `point_history` 단일 테이블 |
| 전체 테이블 실험 | 필수 SQL-only evidence로 수행 |
| API 실험 | hot user의 **Point History** 목록으로 수행 |
| seed 조건 | 기존 `loadtest` 유지 |
| Offset API | 기존 `GET /api/points` 유지 |
| Cursor API | `GET /api/points/cursor` 신규 추가 |
| Cursor 파라미터 | `lastCreatedAt`, `lastId` 명시 파라미터 |
| 정렬 기준 | `created_at DESC, id DESC` |
| 전체 테이블 인덱스 | `idx_point_history_created_id(created_at DESC, id DESC)` |
| hot user 인덱스 | `idx_point_history_user_created_id(user_id, created_at DESC, id DESC)` |
| k6 대상 | hot user API 실험만 |
| Grafana | shared `DB Lab Overview`를 보조 evidence로 사용하고 새 대시보드 파일은 만들지 않음 |

## Vertical Slices

순서대로 진행한다. 각 slice는 리뷰 가능한 독립 변경 단위다.

| 순서 | 문서 | 결과 |
|---:|---|---|
| 001 | [Phase Documentation Scaffold](./001-phase-documentation-scaffold.md) | Phase 7 표준 문서와 evidence index 골격 생성 |
| 002 | [SQL Scripts And Data Profile](./002-sql-scripts-and-data-profile.md) | 전체/hot user SQL-only 실험 스크립트와 인덱스 prepare 스크립트 생성 |
| 003 | [Cursor Point API](./003-cursor-point-api.md) | `/api/points/cursor` API와 cursor 응답 DTO 구현 |
| 004 | [k6 Pagination Scenarios](./004-k6-pagination-scenarios.md) | Offset page별 preset과 Cursor k6 시나리오 추가 |
| 005 | [Evidence Capture And Report](./005-evidence-capture-and-report.md) | Phase 7 evidence 수집 절차와 report 작성 |
| 006 | [Hot User Amplification](./006-hot-user-amplification.md) | Phase 7 전용 가상 hot user 100,000건 보강과 추가 evidence 수집 |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | 테스트, 문서 일관성, evidence 구조 최종 검증 |

## File Ownership

| Path | Responsibility |
|---|---|
| `docs/phases/07-pagination/README.md` | Phase 7 허브 문서 |
| `docs/phases/07-pagination/scope.md` | 범위, 제외 범위, 완료 조건 |
| `docs/phases/07-pagination/runbook.md` | seed, SQL, k6, evidence 수집 절차 |
| `docs/phases/07-pagination/observability.md` | k6, Grafana, `EXPLAIN`, `pg_stat_statements` 관측 전략 |
| `docs/phases/07-pagination/report.md` | 결과 요약과 해석 |
| `docs/evidence/phase-07/README.md` | evidence index |
| `scripts/phase-07/*.sql` | 데이터 프로파일, 인덱스 prepare, EXPLAIN 쿼리 |
| `ecommerce/src/main/java/com/dblab/ecommerce/dto/PointHistoryCursor.java` | Cursor 값 DTO |
| `ecommerce/src/main/java/com/dblab/ecommerce/dto/PointHistoryCursorResponse.java` | Cursor API 응답 DTO |
| `ecommerce/src/main/java/com/dblab/ecommerce/repository/PointHistoryRepository.java` | Offset/Page query와 Cursor query |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java` | Offset/Page와 Cursor 목록 조회 서비스 |
| `ecommerce/src/main/java/com/dblab/ecommerce/controller/PointController.java` | `/api/points`, `/api/points/cursor` endpoint |
| `ecommerce/src/test/java/com/dblab/ecommerce/repository/PointHistoryRepositoryTest.java` | Cursor query 동작 테스트 |
| `ecommerce/src/test/java/com/dblab/ecommerce/point/PointCursorApiTest.java` | Cursor API 통합 테스트 |
| `ecommerce/src/test/resources/test-data/point-history-cursor-setup.sql` | Cursor API 테스트 fixture |
| `k6/points-test.js` | 기존 Offset/Page k6 시나리오 |
| `k6/points-cursor-test.js` | Cursor API k6 시나리오 |
| `k6/presets/points-page0.json` | Offset shallow preset |
| `k6/presets/points-mid.json` | Offset mid preset |
| `k6/presets/points-deep.json` | Offset deep preset |
| `k6/presets/points-cursor.json` | Cursor preset |
| `docs/guides/k6-load-testing.md` | Phase 7 k6 실행법 |

## Cross-Slice Invariants

- `docker/postgres/init.sql`에 Phase 7 실험 인덱스를 추가하지 않는다.
- Phase 7 실험 인덱스는 `scripts/phase-07/*prepare*.sql`에서 만들고 관리한다.
- 기존 `/api/points`의 `Page<PointHistoryResponse>` baseline은 유지한다.
- Cursor API는 전체 개수를 반환하지 않는다.
- Cursor API는 `size + 1`개 조회로 `hasNext`를 판단한다.
- Offset과 Cursor 정렬 기준은 항상 `created_at DESC, id DESC`다.
- `created_at`만 cursor로 쓰지 않는다. `id`를 tie-breaker로 함께 사용한다.
- k6 label에는 `userId`, `page`, SQL text 같은 high-cardinality 값을 넣지 않는다.
- page 차이는 `preset` 이름으로 구분한다.
- Grafana screenshot은 보조 evidence다. 핵심 판단은 k6 summary, `EXPLAIN`, `pg_stat_statements`로 한다.

## Verification Policy

코드 변경 slice 이후 `ecommerce/`에서 실행한다.

```bash
rtk gradlew test --tests "*PointHistoryRepositoryTest" --tests "*PointCursorApiTest"
rtk gradlew compileJava
```

문서와 스크립트 변경 후 repository root에서 실행한다.

```bash
rtk rg -n "phase-07|Point History|point_history|/api/points/cursor|idx_point_history_user_created_id|EXPLAIN \\(ANALYZE, BUFFERS\\)" docs scripts k6 ecommerce/src/main ecommerce/src/test
rtk powershell -NoProfile -Command "$paths = @('docs/phases/07-pagination/README.md','docs/phases/07-pagination/scope.md','docs/phases/07-pagination/runbook.md','docs/phases/07-pagination/observability.md','docs/phases/07-pagination/report.md','docs/evidence/phase-07/README.md','scripts/phase-07/00-data-profile.sql','k6/points-cursor-test.js'); $paths | ForEach-Object { if (-not (Test-Path $_)) { throw \"Missing $_\" } }"
```
