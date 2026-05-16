# Phase 1 Load Test Runbook

> 목적: 다른 사람이 같은 조건으로 데이터를 만들고, Spring 서버를 실행하고, k6 부하 테스트를 반복할 수 있게 한다.

## 전체 흐름

```text
1. Docker 인프라 기동
2. 데이터 시딩 preset 선택
3. row count / hot user 분포 확인
4. Spring 서버를 Hikari pool preset으로 실행
5. 시나리오별 k6 부하 테스트 실행
6. k6 결과, pg_stat_statements, Grafana 캡처 저장
```

## 사전 조건

- Docker 또는 OrbStack이 실행 중이어야 한다.
- PostgreSQL 볼륨에 기존 데이터가 있으면 seeder는 데이터를 다시 넣지 않는다.
- Grafana/Prometheus를 보려면 `docker-compose.yml`의 `postgres-exporter`, `prometheus`, `grafana` 서비스 주석을 해제해야 한다. 현재 파일에서는 해당 서비스가 주석 처리되어 있다.
- k6는 로컬 설치본이 있으면 로컬 실행, 없으면 `grafana/k6` Docker 이미지로 실행된다.

## 인프라 기동

프로젝트 루트에서 실행한다.

```bash
docker compose up -d
docker compose ps
```

Grafana를 활성화했다면 다음 주소를 사용한다.

```text
Prometheus: http://localhost:9090
Grafana:    http://localhost:3000
```

Grafana 기본 계정은 `admin / admin` 또는 `admin / 설정된 비밀번호`다. 현재 compose 예시는 `GF_SECURITY_ADMIN_PASSWORD=admin`을 사용한다.

## 데이터 시딩

시딩은 `scripts/seed.sh`로 실행한다.

```bash
./scripts/seed.sh small
./scripts/seed.sh loadtest
```

| preset | Spring profile | 목적 |
|---|---|---|
| `small` | `seeder,seed-small` | 빠른 로컬 확인 |
| `loadtest` | `seeder,seed-loadtest` | Phase 1 부하 테스트 |

시딩 규모는 Spring profile 파일에서 관리한다.

| 파일 | 설명 |
|---|---|
| `ecommerce/src/main/resources/application-seed-small.yaml` | 소량 테스트 데이터 |
| `ecommerce/src/main/resources/application-seed-loadtest.yaml` | 부하 테스트 데이터 |

`loadtest` 기본값은 다음과 같다.

| 항목 | 값 |
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

데이터를 다시 만들려면 볼륨을 지운 뒤 재시딩한다.

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

## 데이터 검증 쿼리

시딩 후 row count를 확인한다.

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

hot user 분포를 확인한다.

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

## Spring 서버 실행

Spring 서버는 `scripts/server.sh`로 실행한다.

```bash
./scripts/server.sh pool5
./scripts/server.sh pool10
./scripts/server.sh pool20
```

| preset | Spring profile | Hikari maximum-pool-size | Hikari minimum-idle |
|---|---|---:|---:|
| `pool5` | `pool5` | 5 | 2 |
| `pool10` | `pool10` | 10 | 5 |
| `pool20` | `pool20` | 20 | 5 |

Hikari pool 값은 서버 재시작 후 적용된다. 같은 부하 테스트를 pool별로 비교하려면 서버를 종료한 뒤 다른 preset으로 다시 실행한다.

## k6 실행

k6는 `k6/run.sh`로 실행한다.

```bash
./k6/run.sh <scenario> <preset>
```

시나리오:

| scenario | 대상 API | 목적 |
|---|---|---|
| `orders` | `GET /api/orders?userId=` | N+1, Hikari pool 점유 |
| `products` | `GET /api/products?categoryId=&status=` | 인덱스 없는 Seq Scan |
| `points` | `GET /api/points?userId=&page=&size=` | Offset deep page 병목 |

preset:

| preset | rate | duration | 목적 |
|---|---:|---|---|
| `smoke` | 5 rps | 1m | API 정상 확인 |
| `baseline` | 50 rps | 5m | 기본 기준선 |
| `stress-100` | 100 rps | 5m | 부하 증가 |
| `stress-200` | 200 rps | 5m | 한계 확인 |
| `points-page0` | 50 rps | 5m | 얕은 페이지 |
| `points-page500` | 50 rps | 5m | 깊은 페이지 |

예시:

```bash
./k6/run.sh orders smoke
./k6/run.sh orders baseline
./k6/run.sh products baseline
./k6/run.sh products stress-100
./k6/run.sh points points-page0
./k6/run.sh points points-page500
```

## 테스트 전 DB 통계 초기화

시나리오별로 `pg_stat_statements` 결과가 섞이지 않게 테스트 직전에 초기화한다.

```bash
docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
docker compose exec postgres psql -U app -d ecommerce -c "VACUUM ANALYZE;"
```

## 권장 실행 순서

처음에는 Hikari pool을 `pool10`으로 고정하고 단일 API 기준선을 먼저 만든다.

```bash
./scripts/server.sh pool10
```

별도 터미널에서:

```bash
docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
docker compose exec postgres psql -U app -d ecommerce -c "VACUUM ANALYZE;"
./k6/run.sh orders baseline

docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
./k6/run.sh products baseline

docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
./k6/run.sh points points-page0

docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
./k6/run.sh points points-page500
```

병목이 보이는 시나리오만 `pool5`, `pool20`으로 반복한다.

## 결과 저장 규칙

결과는 시나리오와 설정이 섞이지 않게 저장한다.

```text
docs/evidence/phase1/
  orders/
    pool10-baseline/
      k6-summary.txt
      pg-stat-statements.txt
      grafana.png
      notes.md
  products/
    pool10-baseline/
  points/
    pool10-page0/
    pool10-page500/
```

`pg_stat_statements`는 다음 쿼리 결과를 저장한다.

```bash
docker compose exec postgres psql -U app -d ecommerce -c "
SELECT calls,
       round(mean_exec_time::numeric, 2) AS mean_ms,
       round(total_exec_time::numeric, 2) AS total_ms,
       rows,
       left(query, 180) AS query
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
"
```

## 주의 사항

- `seed-loadtest`는 데이터가 크다. 로컬 메모리와 디스크 여유를 확인한다.
- Hikari pool을 바꾸려면 Spring 서버를 재시작한다.
- k6 preset은 서버 재시작 없이 바꿀 수 있다.
- `points-page500`은 hot user에게 충분한 포인트 이력이 있어야 병목이 보인다.
- Grafana 지표가 보이지 않으면 Prometheus scrape target과 compose 서비스 활성화 상태를 먼저 확인한다.
