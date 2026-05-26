# Bulk Update Measurement Condition

- 대상 테스트: `OrderBulkUpdateTest`
- 실행 위치: `ecommerce`
- 실행 명령: `rtk gradlew test --tests "*OrderBulkUpdateTest" --rerun-tasks --info`
- 테스트 데이터: `order-test-setup.sql`
- 대상 주문: `orders.id` 100, 101, 102
- 시작 상태: `PENDING`
- 변경 상태: `PREPARING`
- 측정 지표: Hibernate `Statistics#getPrepareStatementCount()`
- 비교 대상:
  - Row-by-row dirty checking: `findByStatus(PENDING)` 후 각 `Orders` 엔티티에 `markPreparing()` 호출, `flush()`
  - JPQL bulk update: `bulkUpdateStatus(PENDING, PREPARING)` 1회 호출
- Persistence context 확인:
  - 주문 100을 먼저 로딩한 뒤 JPQL bulk update를 실행
  - `@Modifying(clearAutomatically = true)` 적용 후 다시 조회했을 때 `PREPARING` 상태가 보이는지 검증
