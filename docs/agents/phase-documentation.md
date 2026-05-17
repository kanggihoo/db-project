# Phase Documentation

Learning Phase 문서를 읽고 갱신할 때의 AI 작업 규칙이다.

## 읽기 규칙

- 모든 `docs/phases/*/README.md`를 읽지 않는다.
- 사용자 요청, 이슈, 브랜치, roadmap 작업에서 대상 Phase를 먼저 확정한다.
- `docs/phases/README.md`를 읽는다.
- `docs/phases/<target-phase>/README.md`만 읽는다.
- 현재 작업에 필요할 때만 대상 Phase README가 연결한 문서를 연다.
- evidence가 필요할 때만 `docs/evidence/README.md`와 `docs/evidence/<phase>/README.md`를 읽는다.
- AI 계획, 스펙, legacy plan을 다룰 때만 `docs/superpowers/README.md`를 읽는다.

대상 Phase가 불명확하면 사용자에게 묻거나 최상위 인덱스만 확인한다. 모든 Phase 디렉토리를 순회하지 않는다.

## 갱신 규칙

각 `docs/phases/<phase>/` 디렉토리는 아래 5개 파일만 둔다.

- `README.md`
- `scope.md`
- `runbook.md`
- `observability.md`
- `report.md`

AI 계획과 legacy 구현 계획은 `docs/superpowers/plans/`에 둔다.

원본 로그, SQL snapshot, Prometheus export, screenshot은 `docs/evidence/<phase>/`에 둔다.

## 완료 체크리스트

Phase 구현 작업 후 필요한 항목만 갱신한다.

- 범위가 바뀌면 `scope.md`를 갱신한다.
- 실행 절차가 바뀌면 `runbook.md`를 갱신한다.
- 지표나 SQL 확인법이 바뀌면 `observability.md`를 갱신한다.
- 결과 해석은 `report.md`에 남긴다.
- 새 evidence가 추가되면 `docs/evidence/<phase>/README.md`를 갱신한다.
- markdown 링크와 Phase 5파일 구조를 확인한다.
