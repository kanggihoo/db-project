# Phase 5 결과 보고서

## 상태

완료.

Phase 5는 기존 상품 검색 API를 유지하면서 학습 증거 비교를 위한 명시적 strategy를 추가했다.

- `strategy=baseline`: Spring Data JPA 엔티티 조회 후 `ProductResponse.from(product)` 변환
- `strategy=querydsl`: QueryDSL DTO projection
- `strategy` 생략: `querydsl` 기본 사용

## 상품 검색

Focused integration evidence는 baseline과 QueryDSL이 공유 `categoryId=200`, `status=ON_SALE` 조건에서 같은 `ProductResponse` 값을 반환함을 보여준다.

| 전략 | Mapping path | SQL shape | SQL count evidence |
|---|---|---|---:|
| `baseline` | Product 엔티티 -> `ProductResponse.from(product)` | 응답에 필요하지 않은 필드를 포함해 Product 엔티티 컬럼 조회 | 1 |
| `querydsl` | QueryDSL projection -> `ProductResponse` | `ProductResponse` 필드인 `id`, `category_id`, `name`, `base_price`, `status`만 projection | 1 |

Evidence:

- [baseline-sql.txt](../../evidence/phase-05/product-search/baseline-sql.txt)
- [querydsl-sql.txt](../../evidence/phase-05/product-search/querydsl-sql.txt)
- [strategy-test-output.txt](../../evidence/phase-05/product-search/strategy-test-output.txt)
- [summary.md](../../evidence/phase-05/product-search/summary.md)

QueryDSL test는 optional predicate가 null일 때 생략되는지도 검증한다.

## Bulk Update

Focused integration evidence는 같은 fixture에서 managed entity dirty checking과 JPQL bulk update를 비교한다.

| 전략 | 변경 row 수 | Hibernate prepareStatementCount | 추가 증거 |
|---|---:|---:|---|
| Row-by-row dirty checking | 3 | 4 | select 1회 + update 3회, `entityUpdateCount=3` |
| JPQL bulk update | 3 | 1 | `updatedRows=3` |

Evidence:

- [loop-update-sql-count.txt](../../evidence/phase-05/bulk-update/loop-update-sql-count.txt)
- [bulk-update-sql-count.txt](../../evidence/phase-05/bulk-update/bulk-update-sql-count.txt)
- [persistence-context-test-output.txt](../../evidence/phase-05/bulk-update/persistence-context-test-output.txt)
- [summary.md](../../evidence/phase-05/bulk-update/summary.md)

Bulk repository는 method-level transaction boundary와 함께 `@Modifying(clearAutomatically = true, flushAutomatically = true)`를 사용한다. Evidence는 bulk update 이후 clear/reload 동작을 기록한다. 먼저 로딩된 주문은 stale `PENDING` 값이 아니라 변경된 `PREPARING` 상태로 다시 로딩된다.

## Evidence Index

필수 Phase 5 evidence는 [docs/evidence/phase-05/README.md](../../evidence/phase-05/README.md)에 정리되어 있다.

k6/Grafana와 `pg_stat_statements`는 이 phase에서 선택 항목이며 closeout 필수 조건이 아니다.

## Phase 6 Handoff

Phase 5는 단순 상품 검색 read path를 최적화하고 비교했다. Phase 6은 Product/Review 집계 쿼리로 이동한다. 다음 질문을 다룬다.

- 상품 리뷰 요약에서 `GROUP BY`와 `HAVING` 동작
- 집계/필터 표현식을 위한 expression index
- 인덱스 전후 실행 계획 비교
- 단순 predicate에서 효과적이던 인덱스가 집계 쿼리에서도 같은 수준으로 효과적인지 여부
