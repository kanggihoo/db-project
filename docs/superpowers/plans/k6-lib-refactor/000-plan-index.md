# k6 Lib Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

현재 scenario별 k6 entrypoint를 유지하면서 반복되는 preset/env/options/check 코드를 `k6/lib/`로 분리한다.

## Spec

구현 기준 문서:

- [k6 Lib Refactor Design](../../specs/2026-06-02-k6-lib-refactor-design.md)

## Plan Decisions

| 질문 | 결정 |
|---|---|
| entrypoint 구조 | `orders-test.js`, `products-test.js`, `points-*`, `review-summary-test.js` 유지 |
| 1차 lib 범위 | `config.js`, `scenarios.js`, `checks.js`만 생성 |
| 첫 적용 대상 | `k6/review-summary-test.js` tracer bullet |
| random helper | 1차 범위에서 제외하고 scenario 파일에 유지 |
| metrics helper | custom k6 metric이 생길 때까지 보류 |
| consistency helper | SQL/Grafana evidence 흐름으로 유지하고 k6로 이동하지 않음 |
| preset 포맷 | 기존 JSON 유지 |
| 실행 인터페이스 | `k6/run.sh`, `npm run k6:evidence`, `make k6-evidence`, `make evidence-capture` 유지 |

## Implementation Slices

순서대로 진행한다. 각 slice는 리뷰 가능한 독립 변경 단위다.

| 순서 | 문서 | 결과 |
|---:|---|---|
| 001 | [k6 Lib Foundation](./001-k6-lib-foundation.md) | `k6/lib/config.js`, `k6/lib/scenarios.js`, `k6/lib/checks.js` 생성 |
| 002 | [Review Summary Tracer Bullet](./002-review-summary-tracer-bullet.md) | `review-summary-test.js`가 새 lib 구조를 사용 |
| 003 | [Core Scenario Expansion](./003-core-scenario-expansion.md) | `orders-test.js`, `products-test.js`, `points-test.js`가 새 lib 구조를 사용 |
| 004 | [Pagination Scenario Expansion](./004-pagination-scenario-expansion.md) | Phase 7 pagination sampling/cursor k6 파일이 새 lib 구조를 사용 |
| 005 | [Verification And Documentation](./005-verification-and-documentation.md) | 검증 명령, 문서, 추적성 정리 |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | 전체 실행 계약과 회귀 위험 최종 점검 |

## File Ownership

| Path | Responsibility |
|---|---|
| `k6/lib/config.js` | preset/env 로딩, common tags, request tags, threshold 기본값 |
| `k6/lib/scenarios.js` | `constant-arrival-rate` k6 options 생성 |
| `k6/lib/checks.js` | status, latency, JSON body shape check helper |
| `k6/review-summary-test.js` | tracer bullet 대상 scenario |
| `k6/orders-test.js` | orders API scenario |
| `k6/products-test.js` | products API scenario |
| `k6/points-test.js` | offset pagination baseline scenario |
| `k6/points-cursor-test.js` | cursor pagination scenario |
| `k6/points-cursor-sampling-test.js` | cursor sampling scenario |
| `k6/points-offset-sampling-test.js` | offset sampling scenario |
| `docs/guides/k6-load-testing.md` | 새 lib 구조와 실행 예시 문서화, 필요한 경우만 수정 |

## Cross-Slice Invariants

- k6 entrypoint 파일을 하나의 통합 runner로 합치지 않는다.
- `__ENV.PRESET`은 preset 파일 경로로 유지한다.
- `__ENV.PRESET_NAME`은 Grafana/Prometheus label 값으로 유지한다.
- k6 label에는 `phase`, `scenario`, `preset`, `pool`만 공통 low-cardinality measurement label로 유지한다.
- `userId`, `page`, `lastId`, SQL text 같은 high-cardinality 값은 k6 label에 넣지 않는다.
- 기존 preset JSON 파일 이름과 구조를 실험 의도 없이 바꾸지 않는다.
- 기존 threshold 수치를 실험 의도 없이 바꾸지 않는다.
- `k6/lib/*.js`에서는 Node 전용 API, npm package, `fs`, `path`, `process`를 사용하지 않는다.
- scenario별 URL 구성과 request parameter 선택은 scenario 파일에 남긴다.

## Verification Policy

lib와 scenario 변경 후 repository root에서 최소 정적 검증을 실행한다.

```bash
rtk proxy node scripts/verify-observability.mjs
rtk rg -n "from './lib/|from './lib/config.js|from './lib/scenarios.js|from './lib/checks.js|PRESET_NAME|expected_response" k6 scripts
```

환경이 준비된 경우 tracer bullet 실행을 확인한다.

```bash
rtk npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --condition refactor-smoke
```

전체 확장 후 가능한 경우 Phase 7 대표 흐름도 확인한다.

```bash
rtk npm run k6:evidence -- --phase phase-07 --scenario points --preset points-page0 --condition refactor-smoke
```
