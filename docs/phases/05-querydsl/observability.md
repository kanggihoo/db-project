# Phase 5 관측 기준

Phase 5 evidence는 작고 반복 가능해야 하며, 측정하려는 비교 지점과 직접 연결되어야 한다.

## 필수 신호

- `GET /api/products?strategy=baseline` 대표 SQL
- `GET /api/products?strategy=querydsl` 대표 SQL
- Hibernate statistics, 특히 `prepareStatementCount`
- focused integration test 출력
- query count, selected column, mapping path를 설명하는 짧은 요약

## 상품 검색 해석

Baseline 검색은 `Product` 엔티티 컬럼을 로딩하고 각 엔티티를 `ProductResponse.from(product)`으로 변환한다.

QueryDSL 검색은 `ProductResponse` 필드로 직접 projection한다.

- `id`
- `categoryId`
- `name`
- `basePrice`
- `status`

두 전략은 공유 fixture 조건에서 같은 `ProductResponse` 값을 반환했고, 둘 다 SQL statement 1개를 사용했다. Phase 5의 의미 있는 차이는 k6 throughput이 아니라 SQL shape와 mapping path다.

QueryDSL optional predicate는 `categoryId` 또는 `status`가 null일 때 생략되어야 한다. Focused test가 이 동작을 검증한다.

## Bulk Update 해석

행 단위 update 비교는 managed entity dirty checking의 SQL count와 Hibernate entity update count를 기록한다.

- `prepareStatementCount=4`
- fixture 기준 select 1회와 update 3회
- `entityUpdateCount=3`

JPQL bulk update 비교는 줄어든 SQL count를 기록한다.

- `prepareStatementCount=1`
- `updatedRows=3`
- `@Modifying(clearAutomatically = true, flushAutomatically = true)`는 bulk update 전 flush를 요청하고 bulk update 후 persistence context를 clear한다.

## 선택 신호

- 대표 상품 검색 SQL의 `EXPLAIN` 출력
- `pg_stat_statements` 요약
- k6 요약
- Grafana screenshot

k6, Grafana, `pg_stat_statements`는 있으면 유용한 맥락이지만 Phase 5 완료 필수 증거는 아니다.
