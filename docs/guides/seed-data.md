# Seed Data Guide

> 더미 데이터 생성 preset과 검증 쿼리를 정리한다.

## Run

```bash
./scripts/seed.sh small
./scripts/seed.sh loadtest
```

| preset | Spring profiles | Purpose |
|---|---|---|
| `small` | `seeder,seed-small` | 빠른 로컬 확인 |
| `loadtest` | `seeder,seed-loadtest` | 부하 테스트용 데이터 |

시딩은 `users` 테이블에 데이터가 있으면 건너뛴다. 같은 preset으로 다시 만들려면 Docker volume을 초기화한다.

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

## Preset Files

| File | Purpose |
|---|---|
| `ecommerce/src/main/resources/application-seed-small.yaml` | 소량 테스트 데이터 |
| `ecommerce/src/main/resources/application-seed-loadtest.yaml` | 부하 테스트 데이터 |

`loadtest` 기본값:

| Key | Value |
|---|---:|
| users | 10,000 |
| categories | 200 |
| coupons | 50 |
| products | 100,000 |
| orders | 500,000 |
| point_history | 2,000,000 |
| delivery_tracking | 1,000,000 |
| hot user ratio | 10% |
| order hot traffic ratio | 70% |
| point hot traffic ratio | 90% |

## Verify Row Counts

```bash
docker compose exec postgres psql -U app -d ecommerce -c "
SELECT 'users' AS table_name, COUNT(*) FROM users
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL SELECT 'delivery_tracking', COUNT(*) FROM delivery_tracking;
"
```

## Verify Hot Users

```bash
docker compose exec postgres psql -U app -d ecommerce -c "
SELECT user_id, COUNT(*) AS order_count
FROM orders
GROUP BY user_id
ORDER BY order_count DESC
LIMIT 20;
"

docker compose exec postgres psql -U app -d ecommerce -c "
SELECT user_id, COUNT(*) AS point_count
FROM point_history
GROUP BY user_id
ORDER BY point_count DESC
LIMIT 20;
"
```

