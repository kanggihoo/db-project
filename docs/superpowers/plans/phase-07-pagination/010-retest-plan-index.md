# Phase 7 Pagination Retest Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Phase 7 재측정을 A/B/C 실험으로 재구성해 Offset/Page depth, Cursor next-slice, COUNT query 비용을 분리해서 검증한다.

**Architecture:** 기존 Phase 7 구현은 유지하되 재측정 전용 SQL, k6 sampling, evidence capture, report 갱신을 010번대 계획으로 분리한다. `user_id=707000` amplified hot user를 기준 데이터로 사용하고, Cursor는 사전 계산한 cursor sample을 입력으로 받아 next-slice 비용만 측정한다.

**Tech Stack:** Spring Boot 4, Spring Data JPA, PostgreSQL, `EXPLAIN (ANALYZE, BUFFERS)`, `pg_stat_statements`, k6 Trend metrics, shared Grafana `DB Lab Overview`, JUnit Jupiter, AssertJ.

---

## Spec

- [Phase 7 페이지네이션 재측정 Spec](../../specs/2026-05-28-phase-7-pagination-retest-spec.md)
- [기존 Phase 7 구현 계획](./000-plan-index.md)
- [기존 hot user amplification 계획](./006-hot-user-amplification.md)

## Plan Decisions

| 질문 | 결정 |
|---|---|
| 재측정 기준 user | `user_id=707000`, `point_history=100000` |
| Offset 측정 | page sample `[0,10,50,100,500,1000,2000,3000,4000,4999]` |
| Cursor 측정 | 같은 logical page sample의 cursor 값을 사전 계산 |
| Cursor source lookup | latency 측정에 포함하지 않음 |
| COUNT query | `pg_stat_statements`와 count-only EXPLAIN으로 분리 |
| Cache 조건 | warm-cache 반복 부하로 기록 |
| Grafana | 새 panel 없이 기존 shared `DB Lab Overview` 캡처만 수행 |
| Report framing | Page와 Cursor의 UX 목적 차이와 trade-off 중심 |

## Retest Steps

| 순서 | 문서 | 결과 |
|---:|---|---|
| 011 | [Hot User Amplification And Preconditions](./011-hot-user-amplification-and-preconditions.md) | `707000` hot user 생성/검증과 실험 전제 확인 |
| 012 | [Offset Ordering And Tests](./012-offset-ordering-and-tests.md) | Offset API를 Cursor와 같은 정렬 기준으로 맞추고 테스트 보강 |
| 013 | [Retest SQL Scripts And Cursor Samples](./013-retest-sql-scripts-and-cursor-samples.md) | A/B/C SQL-only evidence와 cursor sample 생성 스크립트 추가 |
| 014 | [k6 Sampling Scenarios](./014-k6-sampling-scenarios.md) | Offset/Cursor page sample별 Trend metric k6 시나리오 추가 |
| 015 | [Evidence Capture And Grafana](./015-evidence-capture-and-grafana.md) | k6, `pg_stat_statements`, Grafana screenshot evidence 수집 |
| 016 | [Report And Evidence Docs](./016-report-and-evidence-docs.md) | Phase 7 report와 evidence index를 A/B/C 구조로 갱신 |
| 017 | [Retest Verification Stabilization](./017-retest-verification-stabilization.md) | 테스트, 문서 링크, evidence 구조 최종 검증 |

## File Ownership

| Path | Responsibility |
|---|---|
| `scripts/phase-07/05-hot-user-amplify.sql` | `707000` hot user와 100,000건 `point_history` 생성 |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java` | Offset/Page 정렬 기준 적용 |
| `ecommerce/src/test/java/com/dblab/ecommerce/point/PointCursorApiTest.java` | Offset 정렬과 Cursor API 동작 검증 |
| `scripts/phase-07/31-retest-offset-sampling-explain.sql` | Offset sample page별 EXPLAIN |
| `scripts/phase-07/32-retest-cursor-samples.sql` | Cursor sample 값 생성 |
| `scripts/phase-07/33-retest-cursor-next-slice-explain.sql` | Cursor next-slice EXPLAIN |
| `scripts/phase-07/34-retest-count-only-explain.sql` | count-only EXPLAIN |
| `k6/points-offset-sampling-test.js` | Offset page sample별 k6 Trend |
| `k6/points-cursor-sampling-test.js` | Cursor page sample별 k6 Trend |
| `k6/presets/points-offset-sampling.json` | Offset sampling preset |
| `k6/presets/points-cursor-sampling.json` | Cursor sampling preset with precomputed samples |
| `docs/evidence/phase-07/**` | 재측정 원본 evidence |
| `docs/phases/07-pagination/report.md` | 재측정 결과 해석 |
| `docs/evidence/phase-07/README.md` | evidence index |

## Cross-Step Invariants

- Offset과 Cursor는 모두 `created_at DESC, id DESC` logical order를 사용한다.
- Cursor sampling은 iteration당 하나의 cursor request만 보낸다.
- Cursor sample lookup은 측정 준비 단계이며 API latency에 포함하지 않는다.
- `pg_stat_statements_reset()`은 통계 분리용이며 cache 초기화로 해석하지 않는다.
- Grafana screenshot은 보조 evidence다.
- Phase 7의 primary evidence는 `EXPLAIN`, k6 summary, `pg_stat_statements` snapshot이다.

