# 상품 검색 측정 조건

| 항목 | 값 |
|---|---|
| Phase | 05-querydsl |
| Scenario | product-search |
| Database | PostgreSQL Testcontainers |
| Fixture | `ecommerce/src/test/resources/test-data/product-setup.sql` |
| Baseline strategy | `baseline` |
| QueryDSL strategy | `querydsl` |
| 공유 조건 | `categoryId=200`, `status=ON_SALE` |
| 필수 도구 | JUnit, Hibernate statistics, Hibernate SQL logs |
| k6/Grafana | 사용하지 않음 |
