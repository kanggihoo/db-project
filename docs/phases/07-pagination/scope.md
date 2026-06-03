# Phase 7 Scope

## 목표

`point_history`에서 Offset deep page, Cursor pagination, COUNT 쿼리 비용을 비교한다.

## 포함 범위

- 전체 `point_history` SQL-only Offset/Cursor 실행계획 비교
- hot user의 **Point History** API/k6 비교
- `GET /api/points/cursor` 추가
- `Page` 방식 COUNT 쿼리와 Cursor/Slice 방식의 count 없는 조회 비교
- Phase 7 evidence를 `docs/evidence/phase-07/`에 기록

## 제외 범위

- **Delivery Tracking** 필수 실험
- seed preset 변경
- 사용자-facing 전체 `point_history` 목록 API
- 프론트엔드 또는 페이지 번호 UI
- PostgreSQL 외 RDBMS 비교

## 완료 조건

- [ ] 전체 `point_history` SQL-only 실험에서 Offset shallow/deep과 Cursor deep 실행계획을 비교했다.
- [ ] hot user 후보와 선정 user의 `point_count`, `midPage`, `deepPage`를 기록했다.
- [ ] Offset `page=0`, `midPage`, `deepPage` k6 p95를 비교했다.
- [ ] Cursor API k6 p95를 기록했다.
- [ ] `pg_stat_statements`로 Offset/Page count query와 Cursor API의 count query 제거 여부를 확인했다.
- [ ] 결과를 `report.md`와 `docs/evidence/phase-07/README.md`에 연결했다.
