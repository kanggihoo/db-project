# 상품 검색 요약

## 결과

Baseline과 QueryDSL 전략은 공유 `categoryId`, `status` 조건에서 같은 `ProductResponse` 값을 반환했다.

## SQL Shape

- Baseline은 `ProductResponse`로 변환하기 전에 Product 엔티티 컬럼을 조회했다.
- QueryDSL은 `ProductResponse`에 필요한 필드만 조회했다.

## SQL Count

두 전략은 공유 조건에서 각각 SQL statement 1개를 사용했다. Phase 5의 차이는 load-test throughput이 아니라 선택 컬럼 shape와 DTO projection 경로다.

## Null 조건

Focused test는 QueryDSL이 null `categoryId`와 null `status` predicate를 생략함을 검증했다.
