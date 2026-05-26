# Bulk Update 요약

## 결과

| 전략 | 변경 row 수 | Hibernate prepareStatementCount |
|---|---:|---:|
| Row-by-row dirty checking | 3 | 4 |
| JPQL bulk update | 3 | 1 |

## 해석

Row-by-row dirty checking은 먼저 대상 주문을 조회한 뒤 변경된 각 엔티티를 flush한다. 이 fixture/configuration에서는 주문 3개가 변경되었고, Hibernate는 `entityUpdateCount=3`, `prepareStatementCount=4`를 기록했다.

JPQL bulk update는 같은 3개 row를 `prepareStatementCount=1`로 변경했다. 이 evidence는 해당 상태 전이 테스트에서 bulk update 경로가 엔티티별 dirty checking보다 더 적은 prepared statement를 사용함을 보여준다.

## Persistence Context 동작

Repository 메서드는 method-level transaction boundary와 함께 `@Modifying(clearAutomatically = true, flushAutomatically = true)`를 사용한다. Focused persistence-context evidence는 clear/reload 동작을 검증한다. Bulk update 이후 먼저 로딩된 주문은 데이터베이스에서 다시 조회되며 stale `PENDING` 값이 아니라 `PREPARING` 상태를 보여준다.
