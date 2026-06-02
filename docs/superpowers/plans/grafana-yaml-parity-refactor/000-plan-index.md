# Grafana YAML Parity Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

`DB Lab Overview` dashboard의 생성 결과와 실행 계약을 유지하면서 `scripts/generate-db-lab-dashboard.mjs` monolith를 YAML source + compiler 구조로 전환한다.

## Spec

구현 기준 문서:

- [Grafana YAML Parity Refactor Design](../../specs/2026-06-02-grafana-yaml-parity-refactor-design.md)

## Plan Decisions

| 질문 | 결정 |
|---|---|
| 전환 방식 | parity-first YAML 전환 |
| dashboard uid | `db-lab-overview` 유지 |
| dashboard title | `DB Lab Overview` 유지 |
| output path | `docker/grafana/dashboards/db-lab-overview.json` 유지 |
| 기존 generator | `scripts/generate-db-lab-dashboard.mjs`를 compatibility wrapper로 유지 |
| YAML 위치 | `scripts/grafana/**/*.yml` |
| compiler 위치 | `scripts/grafana/lib/*.mjs` |
| row/panel 개선 | 이번 범위에서 제외 |
| capture script 계약 | 변경하지 않음 |
| 검증 기준 | `make grafana-generate`, `scripts/verify-observability.mjs` 통과 |

## Implementation Slices

순서대로 진행한다. 각 slice는 리뷰 가능한 독립 변경 단위다.

| 순서 | 문서 | 결과 |
|---:|---|---|
| 001 | [Baseline Contract And Dependency](./001-baseline-contract-and-dependency.md) | 현재 dashboard 계약과 YAML dependency 준비 |
| 002 | [YAML Source Extraction](./002-yaml-source-extraction.md) | 기존 dashboard metadata, rows, queries를 YAML로 이동 |
| 003 | [Compiler Foundation](./003-compiler-foundation.md) | YAML loader, query registry, builder, layout, writer 구현 |
| 004 | [Parity Generation Wrapper](./004-parity-generation-wrapper.md) | 기존 generator를 wrapper로 바꾸고 YAML compiler로 JSON 생성 |
| 005 | [Verifier And Command Compatibility](./005-verifier-and-command-compatibility.md) | verifier와 명령 계약 검증 |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | 전체 parity, capture 영향, diff 최종 점검 |

## File Ownership

| Path | Responsibility |
|---|---|
| `scripts/generate-db-lab-dashboard.mjs` | 기존 명령 호환 wrapper |
| `scripts/grafana/generate.mjs` | YAML dashboard generation entrypoint |
| `scripts/grafana/dashboards/db-lab-overview.yml` | dashboard metadata, variables, row order |
| `scripts/grafana/rows/*.yml` | row별 panel 정의 |
| `scripts/grafana/queries/*.yml` | PromQL alias 정의 |
| `scripts/grafana/lib/yaml-loader.mjs` | YAML loading/parsing |
| `scripts/grafana/lib/query-registry.mjs` | query alias registry와 validation |
| `scripts/grafana/lib/grafana-builder.mjs` | Grafana JSON panel/target builder |
| `scripts/grafana/lib/layout.mjs` | grid position 계산 |
| `scripts/grafana/lib/dashboard-compiler.mjs` | dashboard YAML + row YAML + query registry를 JSON으로 compile |
| `scripts/grafana/lib/write-dashboard.mjs` | output directory 생성과 JSON write |
| `scripts/verify-observability.mjs` | generated dashboard 계약 검증 |
| `package.json` | `yaml` devDependency |
| `package-lock.json` | dependency lock |

## Cross-Slice Invariants

- `docker/grafana/dashboards/db-lab-overview.json` output path를 바꾸지 않는다.
- dashboard `uid: db-lab-overview`를 바꾸지 않는다.
- dashboard `title: DB Lab Overview`를 바꾸지 않는다.
- dashboard variables `phase`, `scenario`, `preset`, `pool`, `uri`, `table`을 유지한다.
- PromQL 의미를 리팩토링 목적 없이 바꾸지 않는다.
- row title과 row 순서를 가능한 한 유지한다.
- capture script CLI 계약을 바꾸지 않는다.
- Grafana provisioning path를 바꾸지 않는다.
- generated JSON을 사람이 직접 수정하는 source of truth로 삼지 않는다.
- 이번 단계에서 row/panel 개선을 하지 않는다.

## Verification Policy

각 slice 후 가능한 범위에서 repository root에서 검증한다.

```bash
rtk proxy node scripts/generate-db-lab-dashboard.mjs
rtk proxy node scripts/verify-observability.mjs
```

최종 slice에서는 Makefile target도 확인한다.

```bash
rtk proxy make grafana-generate
rtk proxy node scripts/verify-observability.mjs
rtk git diff --check
```
