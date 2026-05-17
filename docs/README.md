# Project Documentation

이 디렉토리는 이커머스 DB 최적화 학습 프로젝트의 로드맵, Phase별 작업 문서, 증빙 자료, SQL 학습 자료를 관리한다.

## Structure

| Path | Purpose |
|---|---|
| `roadmap/` | 전체 로드맵과 Phase별 목표, 완료 조건 |
| `guides/` | 여러 Phase에서 반복 사용하는 실행법과 관측 기준 |
| `phases/` | Phase별 계획, 구현 기록, 실행 가이드, 관측 전략 |
| `evidence/` | 부하 테스트 결과, Grafana 캡처, SQL 분석 결과 |
| `sql-quiz/` | SQL 학습 문제와 풀이 자료 |
| `agents/` | 에이전트 스킬이 참조하는 issue tracker, triage label, domain docs 설정 |

## Phase Documents

| Phase | Documents |
|---|---|
| Phase 0. Setup | [plan](./phases/00-setup/plan.md), [implementation](./phases/00-setup/implementation.md) |
| Phase 1. Baseline | [overview](./phases/01-baseline/overview.md), [plan](./phases/01-baseline/plan.md), [implementation](./phases/01-baseline/implementation.md), [runbook](./phases/01-baseline/runbook.md), [observability](./phases/01-baseline/observability.md) |

## Common Guides

- [Environment](./guides/environment.md)
- [Scripts](./guides/scripts.md)
- [Seed Data](./guides/seed-data.md)
- [Spring Profiles](./guides/spring-profiles.md)
- [k6 Load Testing](./guides/k6-load-testing.md)
- [Grafana Observability](./guides/grafana-observability.md)
- [DB Lab Grafana Dashboard Spec](./guides/db-lab-grafana-dashboard-spec.md)
- [Agentic Issue Workflow](./guides/agentic-issue-workflow.md)

## Roadmap

- [Overview](./roadmap/00-overview.md)
- [Phase 0. Project Setup](./roadmap/01-phase-0-setup.md)
- [Phase 1. Baseline](./roadmap/02-phase-1-baseline.md)

## Conventions

- `roadmap/`에는 목표와 완료 조건을 둔다.
- `phases/NN-name/`에는 해당 Phase를 실제로 수행하기 위한 문서를 둔다.
- 반복 사용되는 실행법이나 운영 규칙은 `guides/`에 둔다.
- 측정 결과와 스크린샷은 문서 본문이 아니라 `evidence/` 아래에 저장한다.
