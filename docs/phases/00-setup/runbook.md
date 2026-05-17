# Phase 0 실행 절차

## 사전 조건

- Docker가 실행 중이어야 한다.
- 프로젝트 루트에서 명령을 실행한다.
- 공통 환경 정보는 [Environment Guide](../../guides/environment.md)를 따른다.

## 인프라 기동

```bash
docker compose up -d
docker compose ps
```

접속 정보:

| 서비스 | 주소 |
|---|---|
| PostgreSQL | `localhost:5432`, database `ecommerce`, user `app` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

## 컴파일과 테스트

```bash
cd ecommerce
./gradlew compileJava
./gradlew test
```

시딩 통합 테스트만 실행할 때:

```bash
cd ecommerce
./gradlew test --tests "com.dblab.ecommerce.seeder.DataSeederIntegrationTest"
```

## 데이터 시딩

시딩은 `scripts/seed.sh`를 기준으로 실행한다.

```bash
./scripts/seed.sh small
./scripts/seed.sh loadtest
```

직접 Spring profile을 지정해야 할 때:

```bash
cd ecommerce
./gradlew bootRun --args='--spring.profiles.active=seeder'
```

## 데이터 검증

```bash
docker compose exec postgres psql -U app -d ecommerce
```

주요 테이블 건수를 확인한다.

```sql
SELECT 'users' AS tbl, COUNT(*) FROM users
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL SELECT 'delivery_tracking', COUNT(*) FROM delivery_tracking;
```

FK 정합성을 확인한다.

```sql
SELECT 'order_item orphan', COUNT(*)
FROM order_item oi
LEFT JOIN orders o ON o.id = oi.order_id
WHERE o.id IS NULL;
```

## 데이터 초기화

볼륨까지 삭제하면 PostgreSQL 데이터가 모두 사라진다.

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh small
```

## 다음 단계

Phase 0 검증 후 Phase 1에서 `loadtest` 데이터와 Hikari pool preset을 고정해 Baseline을 측정한다.
