# Phase 문서 규칙

`docs/phases/<phase>/`는 각 Learning Phase를 이해하고, 실행하고, 결과를 판정하기 위한 현재 유효 문서만 둔다.

## 표준 파일

| 파일 | 목적 |
|---|---|
| `README.md` | Phase 허브. 목적, 상태, 주요 문서와 증거 링크를 모은다. |
| `scope.md` | Phase 범위. 목표, 대상 API/테이블, 제외 범위, 완료 조건을 정의한다. |
| `runbook.md` | 실행 절차. 같은 조건으로 재현하기 위한 명령과 순서를 기록한다. |
| `observability.md` | 관측 전략. 어떤 지표와 SQL을 보고 어떻게 해석할지 정리한다. |
| `report.md` | 결과 보고서. 측정값 해석, 병목 분류, 다음 Phase로 넘길 인사이트를 기록한다. |

## 분리 규칙

- Phase 수행 중 사용하는 반복 실행법은 `docs/guides/`에 둔다.
- 부하 테스트 결과, SQL snapshot, Grafana screenshot은 `docs/evidence/`에 둔다.
- AI 작업 계획, vertical slice 계획, 과거 구현 계획은 `docs/superpowers/plans/`에 둔다.
- Phase 목표와 순서는 `docs/roadmap/`에서 관리한다.
