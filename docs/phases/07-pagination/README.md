# Phase 7. 페이지네이션 최적화

Phase 7은 **Point History** 단일 테이블을 대상으로 Offset pagination의 deep page 비용과 Cursor pagination의 개선 효과를 측정한다.

## 상태

- 상태: planned
- 기준 spec: [2026-05-27-phase-7-pagination-design.md](../../superpowers/specs/2026-05-27-phase-7-pagination-design.md)
- roadmap: [08-phase-7-pagination.md](../../roadmap/08-phase-7-pagination.md)

## 주요 질문

- Offset 방식은 인덱스가 있어도 깊은 페이지에서 왜 느려지는가?
- Cursor 방식은 같은 위치의 다음 페이지를 어떻게 더 적은 skip 비용으로 조회하는가?
- `Page<T>`가 만드는 COUNT 쿼리는 목록 조회에서 어떤 추가 비용을 만드는가?

## 문서

| 문서 | 목적 |
|---|---|
| [scope.md](./scope.md) | 범위, 제외 범위, 완료 조건 |
| [runbook.md](./runbook.md) | 실행 절차 |
| [observability.md](./observability.md) | 관측 지표와 해석 |
| [report.md](./report.md) | 결과 기록 |

## Evidence

- [Phase 7 evidence index](../../evidence/phase-07/README.md)
