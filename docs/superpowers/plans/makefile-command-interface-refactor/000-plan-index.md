# Makefile Command Interface Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 루트 `Makefile`을 얇은 진입점으로 축소하고 현재 공개 명령 계약을 유지한 채 target 구현을 `makefiles/*.mk`로 분리한다.

**Architecture:** 루트 `Makefile`은 shell 설정, 기본 goal, include 선언만 가진다. 실제 target은 `makefiles/config.mk`, `help.mk`, `env.mk`, `db.mk`, `grafana.mk`, `k6.mk`, `evidence.mk`, `sql.mk`에 책임별로 분리한다. 기존 public 변수 `PRESET`은 유지하고, 내부 파생 변수 `K6_PRESET`과 `GRAFANA_PRESET`으로 향후 preset 분리 가능성을 열어둔다.

**Tech Stack:** GNU Make, npm scripts, Node.js CLI wrappers, Docker Compose, Grafana dashboard generator.

---

## Spec

구현 기준 문서:

- [Makefile Command Interface Refactor Design](../../specs/2026-06-02-makefile-command-interface-refactor-design.md)

## Plan Decisions

| 질문 | 결정 |
|---|---|
| 기본 구조 | 루트 `Makefile` thin entrypoint + `makefiles/*.mk` include |
| compatibility file | 이번 범위에서 `phase-compat.mk`를 만들지 않음 |
| Phase 3/4 wrapper | 이번 범위에서 새로 추가하지 않음 |
| public preset 변수 | `PRESET` 유지 |
| 내부 preset 변수 | `K6_PRESET ?= $(PRESET)`, `GRAFANA_PRESET ?= $(PRESET)` |
| k6 target preset | `K6_PRESET` 사용 |
| Grafana capture preset | `GRAFANA_PRESET` 사용 |
| `evidence-capture` preset | 현재 Node wrapper 계약상 `K6_PRESET`을 `--preset`으로 전달 |
| Node wrapper 계약 | 변경하지 않음 |
| 문서 갱신 | `docs/guides/commands.md`, `docs/guides/project-format-standard.md` |

## Implementation Slices

순서대로 진행한다. 각 slice는 리뷰 가능한 독립 변경 단위다.

| 순서 | 문서 | 결과 |
|---:|---|---|
| 001 | [Baseline Command Contract](./001-baseline-command-contract.md) | 현재 Makefile 명령 계약과 dry-run 기준선 확인 |
| 002 | [Makefile Include Split](./002-makefile-include-split.md) | 루트 Makefile을 include 중심으로 축소하고 target을 파일별로 이동 |
| 003 | [Preset Variable Compatibility](./003-preset-variable-compatibility.md) | `PRESET`, `K6_PRESET`, `GRAFANA_PRESET` 호환 검증 |
| 004 | [Help And Command Docs](./004-help-and-command-docs.md) | help 출력과 command guide 갱신 |
| 005 | [Dry Run And Verification](./005-dry-run-and-verification.md) | 핵심 target dry-run과 저비용 검증 |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | 전체 diff, 문서, 실행 계약 최종 점검 |

## File Ownership

| Path | Responsibility |
|---|---|
| `Makefile` | shell 설정, `.DEFAULT_GOAL`, include 순서 |
| `makefiles/config.mk` | 공통 변수 기본값, preset 파생 변수, argument fragment |
| `makefiles/help.mk` | `help` target 출력 |
| `makefiles/env.mk` | `env-check` |
| `makefiles/db.mk` | `db-start`, `db-shell` |
| `makefiles/grafana.mk` | `grafana-generate`, `grafana-capture` |
| `makefiles/k6.mk` | `k6-evidence` |
| `makefiles/evidence.mk` | `evidence-capture`, `phase-status` |
| `makefiles/sql.mk` | `phase-sql` |
| `docs/guides/commands.md` | 현재 프로젝트 command guide |
| `docs/guides/project-format-standard.md` | 프로젝트 간 Makefile command standard |

## Cross-Slice Invariants

- 기존 public target 이름을 바꾸지 않는다.
- 기존 `PRESET=...` 호출 방식을 깨지 않는다.
- `scripts/run-k6-evidence.mjs` CLI 계약을 바꾸지 않는다.
- `scripts/capture-grafana-dashboard.mjs` CLI 계약을 바꾸지 않는다.
- `scripts/run-phase-sql.mjs` CLI 계약을 바꾸지 않는다.
- 이번 범위에서 `phase-compat.mk`를 만들지 않는다.
- 이번 범위에서 Phase 3/4 compatibility target을 새로 추가하지 않는다.
- 이번 범위에서 k6, Grafana YAML, SQL runner 내부 구현을 리팩토링하지 않는다.

## Verification Policy

각 slice 후 가능한 범위에서 repository root에서 검증한다.

```bash
rtk proxy make help
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index TABLE=review
```

최종 slice에서는 다음도 확인한다.

```bash
rtk proxy make grafana-generate
rtk proxy node scripts/verify-observability.mjs
rtk git diff --check
```
