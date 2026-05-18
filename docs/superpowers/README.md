# Superpowers 산출물

이 디렉토리는 superpowers 스킬을 사용하거나 AI 작업 계획을 만들 때 생긴 산출물을 보관한다. 사람이 반복해서 따라 보는 실행 가이드는 `docs/guides/`, 현재 유효한 Phase 문서는 `docs/phases/`에 둔다.

## 구조

| 경로 | 목적 |
|---|---|
| `specs/` | 기능이나 문서 작업의 요구사항 명세 |
| `plans/` | 구현 계획, vertical slice 계획, 과거 계획 산출물 |

## 현재 산출물

- [DB Lab Grafana Dashboard Spec](./specs/db-lab-grafana-dashboard-spec.md)
- [Phase 2 Index Optimization Spec](./specs/phase-2-index-optimization-spec.md)
- [DB Lab Grafana Dashboard Plan](./plans/db-lab-grafana-dashboard/000-plan-index.md)
- [Phase 2 Index Optimization Plan](./plans/phase-2-index-optimization/000-plan-index.md)
- [Phase 0 Legacy Plan](./plans/phase-00-setup/000-legacy-plan.md)
- [Phase 1 Legacy Plan](./plans/phase-01-baseline/000-legacy-plan.md)

## 규칙

- `docs/phases/<phase>/`에는 현재 실행과 판단에 필요한 5개 문서만 둔다.
- 과거 계획 문서는 삭제하지 않고 `plans/<topic>/000-legacy-plan.md`로 옮겨 맥락을 보존한다.
- superpowers 계획 문서는 실행 결과가 아니라 작업 의도와 분해 방식을 남기는 자료로 취급한다.
