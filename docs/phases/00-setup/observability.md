# Phase 0 관측 기준

## 목적

Phase 0에서는 성능 개선 효과를 측정하지 않는다. 대신 이후 Phase가 같은 기반 위에서 실행될 수 있도록 인프라, DB 스키마, 시딩 결과, 관측 도구가 정상인지 확인한다.

## 확인 항목

| 항목 | 확인 방법 | 정상 기준 |
|---|---|---|
| 컨테이너 상태 | `docker compose ps` | PostgreSQL, Prometheus, Grafana, postgres-exporter가 실행 중 |
| PostgreSQL 접속 | `psql -U app -d ecommerce` | DB 접속 가능 |
| `pg_stat_statements` | `SELECT * FROM pg_extension WHERE extname = 'pg_stat_statements';` | extension 존재 |
| Prometheus target | `http://localhost:9090/targets` | PostgreSQL exporter와 Spring target scrape 가능 |
| Grafana datasource | Grafana datasource 설정 | Prometheus datasource가 연결됨 |
| row count | 주요 테이블 `COUNT(*)` | seed preset 기대 규모와 일치 |
| FK 정합성 | orphan 확인 SQL | orphan count 0 |

## 주요 SQL

`pg_stat_statements` extension 확인:

```sql
SELECT *
FROM pg_extension
WHERE extname = 'pg_stat_statements';
```

테이블 규모 확인:

```sql
SELECT 'users' AS tbl, COUNT(*) FROM users
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL SELECT 'delivery_tracking', COUNT(*) FROM delivery_tracking;
```

## 주의 사항

- Testcontainers 환경에서는 `pg_stat_statements`가 운영 Docker Compose와 다르게 동작할 수 있다.
- Hikari pool과 k6 label 기반 관측은 Phase 1 이후 문서에서 다룬다.
- Grafana dashboard의 Phase Evidence 캡처는 `docs/evidence/`에 저장한다.
