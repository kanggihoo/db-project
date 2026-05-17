# 프로젝트 문서

이 디렉토리는 이커머스 DB 최적화 학습 프로젝트의 로드맵, Phase별 작업 문서, 증빙 자료, SQL 학습 자료를 관리한다.

## 구조

| Path | Purpose |
|---|---|
| `roadmap/` | 전체 로드맵과 Phase별 목표, 완료 조건 |
| `guides/` | 여러 Phase에서 반복 사용하는 실행법과 관측 기준 |
| `phases/` | Phase별 범위, 실행 절차, 관측 전략, 결과 보고서 |
| `evidence/` | 부하 테스트 결과, Grafana 캡처, SQL 분석 결과 |
| `superpowers/` | AI 작업 계획, 스펙, vertical slice 산출물 |
| `sql-quiz/` | SQL 학습 문제와 풀이 자료 |
| `adr/` | AI와 사람이 함께 보는 프로젝트 의사결정 기록 |
| `agents/` | 에이전트 스킬이 참조하는 issue tracker, triage label, domain docs 설정 |

## Phase 문서

| Phase | Documents |
|---|---|
| Phase 0. Setup | [README](./phases/00-setup/README.md), [scope](./phases/00-setup/scope.md), [runbook](./phases/00-setup/runbook.md), [observability](./phases/00-setup/observability.md), [report](./phases/00-setup/report.md) |
| Phase 1. Baseline | [README](./phases/01-baseline/README.md), [scope](./phases/01-baseline/scope.md), [runbook](./phases/01-baseline/runbook.md), [observability](./phases/01-baseline/observability.md), [report](./phases/01-baseline/report.md) |

표준 규칙은 [phases/README.md](./phases/README.md)를 따른다.

## 공통 가이드

- [Environment](./guides/environment.md)
- [Scripts](./guides/scripts.md)
- [Seed Data](./guides/seed-data.md)
- [Spring Profiles](./guides/spring-profiles.md)
- [k6 Load Testing](./guides/k6-load-testing.md)
- [Grafana Observability](./guides/grafana-observability.md)
- [Agentic Issue Workflow](./guides/agentic-issue-workflow.md)

## Roadmap

- [Overview](./roadmap/00-overview.md)
- [Phase 0. Project Setup](./roadmap/01-phase-0-setup.md)
- [Phase 1. Baseline](./roadmap/02-phase-1-baseline.md)

## 증빙 자료

- [Evidence README](./evidence/README.md)
- [Phase 1 Evidence](./evidence/phase-01/README.md)

## Superpowers 산출물

- [Superpowers README](./superpowers/README.md)
- [DB Lab Grafana Dashboard Spec](./superpowers/specs/db-lab-grafana-dashboard-spec.md)
- [Phase 0 Legacy Plan](./superpowers/plans/phase-00-setup/000-legacy-plan.md)
- [Phase 1 Legacy Plan](./superpowers/plans/phase-01-baseline/000-legacy-plan.md)

## 규칙

- `roadmap/`에는 목표와 완료 조건을 둔다.
- `phases/NN-name/`에는 `README.md`, `scope.md`, `runbook.md`, `observability.md`, `report.md`만 둔다.
- 반복 사용되는 실행법이나 운영 규칙은 `guides/`에 둔다.
- 측정 결과와 스크린샷은 문서 본문이 아니라 `evidence/` 아래에 저장한다.
- AI 작업 계획과 과거 구현 계획은 `superpowers/` 아래에 저장한다.
