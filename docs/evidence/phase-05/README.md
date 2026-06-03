# Phase 05 Evidence

Phase 5 evidence는 엔티티 기반 상품 검색 baseline과 QueryDSL DTO projection을 비교하고, 주문 상태 변경의 행 단위 update와 JPQL bulk update를 비교한다.

## 상품 검색

| Evidence | 경로 | 결과 |
|---|---|---|
| 측정 조건 | [product-search/measurement-condition.md](./product-search/measurement-condition.md) | `categoryId=200`, `status=ON_SALE`, PostgreSQL Testcontainers |
| Baseline SQL | [product-search/baseline-sql.txt](./product-search/baseline-sql.txt) | `ProductResponse.from(product)` 변환 전에 Product 엔티티 컬럼 조회 |
| QueryDSL SQL | [product-search/querydsl-sql.txt](./product-search/querydsl-sql.txt) | `ProductResponse` 필드만 직접 projection |
| 테스트 출력 | [product-search/strategy-test-output.txt](./product-search/strategy-test-output.txt) | `PHASE5_PRODUCT_SEARCH_BASELINE_SQL_COUNT=1`, `PHASE5_PRODUCT_SEARCH_QUERYDSL_SQL_COUNT=1` |
| 요약 | [product-search/summary.md](./product-search/summary.md) | 같은 `ProductResponse` 값 반환, QueryDSL null predicate 생략 |

## Bulk Update

| Evidence | 경로 | 결과 |
|---|---|---|
| 측정 조건 | [bulk-update/measurement-condition.md](./bulk-update/measurement-condition.md) | 주문 상태 fixture에서 `PENDING` -> `PREPARING` |
| 행 단위 SQL count | [bulk-update/loop-update-sql-count.txt](./bulk-update/loop-update-sql-count.txt) | `PHASE5_ROW_BY_ROW_UPDATE_SQL_COUNT=4`, `entityUpdateCount=3` |
| Bulk update SQL count | [bulk-update/bulk-update-sql-count.txt](./bulk-update/bulk-update-sql-count.txt) | `PHASE5_BULK_UPDATE_SQL_COUNT=1`, `updatedRows=3` |
| 테스트 출력 | [bulk-update/persistence-context-test-output.txt](./bulk-update/persistence-context-test-output.txt) | focused integration test 출력 |
| 요약 | [bulk-update/summary.md](./bulk-update/summary.md) | Bulk update가 더 적은 prepared statement를 사용하고 persistence context를 clear |

## 선택 증거

k6/Grafana와 `pg_stat_statements`는 이후 더 넓은 runtime 관측을 위해 추가할 수 있지만 Phase 5 closeout 필수 조건은 아니다.
