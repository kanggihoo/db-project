# Product Search Measurement Condition

| Field | Value |
|---|---|
| Phase | 05-querydsl |
| Scenario | product-search |
| Database | PostgreSQL Testcontainers |
| Fixture | `ecommerce/src/test/resources/test-data/product-setup.sql` |
| Baseline strategy | `baseline` |
| QueryDSL strategy | `querydsl` |
| Shared conditions | `categoryId=200`, `status=ON_SALE` |
| Required tools | JUnit, Hibernate statistics, Hibernate SQL logs |
| k6/Grafana | Not used |
