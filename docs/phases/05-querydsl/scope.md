# Phase 5 범위

Phase 5는 비교 가능한 baseline 경로를 유지하면서 QueryDSL DTO projection이 상품 검색 read path를 어떻게 바꾸는지 검증한다.

## 포함 범위

- `GET /api/products`
- `strategy=baseline|querydsl`
- `strategy` 생략 시 `querydsl` 기본 사용
- optional `categoryId`, `status` request parameter
- baseline 경로: Spring Data JPA 엔티티 조회 후 `ProductResponse.from(product)` 변환
- QueryDSL 경로: `ProductResponse` DTO projection
- optional 조건이 null일 때 QueryDSL predicate 생략
- 행 단위 update와 JPQL bulk update 비교
- [docs/evidence/phase-05](../../evidence/phase-05/README.md) 아래 evidence 저장

## 제외 범위

- 추가 가격 범위 또는 텍스트 검색 필터
- k6 또는 Grafana 필수 증거
- `pg_stat_statements` 필수 증거
- 재고, 쿠폰, 주문 동시성 제어
- lock, retry, idempotency 전략
- 다른 사용자-facing 상품 검색 API 변경

## 완료 체크리스트

- [x] `GET /api/products`가 `strategy=baseline|querydsl`을 받는다.
- [x] `strategy` 생략 시 `querydsl`을 기본값으로 사용한다.
- [x] baseline과 QueryDSL이 공유 `categoryId`, `status` 조건에서 같은 `ProductResponse` 값을 반환한다.
- [x] QueryDSL null predicate 생략을 focused test로 검증했다.
- [x] baseline SQL shape와 QueryDSL SQL shape를 캡처했다.
- [x] 행 단위 update와 bulk update의 SQL count evidence를 캡처했다.
- [x] Bulk update persistence context 동작을 검증했다.
- [x] evidence를 `docs/evidence/phase-05/` 아래에 정리했다.
- [x] Phase 6 handoff를 report에 기록했다.
