# Phase 1 부하 테스트 실행 절차

> 목적: 다른 사람이 같은 조건으로 데이터를 만들고, Spring 서버를 실행하고, k6 부하 테스트를 반복할 수 있게 한다.

## 전체 흐름

```text
1. Docker 인프라 기동
2. 데이터 시딩 preset 선택
3. row count / hot user 분포 확인
4. Spring 서버를 Hikari pool preset으로 실행
5. 시나리오별 k6 부하 테스트 실행
6. k6 결과, pg_stat_statements, SQL snapshot 저장
```

## 사전 조건

- Docker/PostgreSQL 기본 실행은 [Environment Guide](../../guides/environment.md)를 따른다.
- 데이터 시딩 preset과 검증 쿼리는 [Seed Data Guide](../../guides/seed-data.md)를 따른다.
- Spring profile과 Hikari pool preset은 [Spring Profiles Guide](../../guides/spring-profiles.md)를 따른다.
- k6 runner와 preset 사용법은 [k6 Load Testing Guide](../../guides/k6-load-testing.md)를 따른다.

## 인프라 기동

프로젝트 루트에서 실행한다.

```bash
docker compose up -d
docker compose ps
```

Prometheus 접근 정보는 [Environment Guide](../../guides/environment.md)를 기준으로 한다.

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

시딩 규모와 재시딩 방법은 [Seed Data Guide](../../guides/seed-data.md)를 기준으로 한다.

## 데이터 검증 쿼리

시딩 후 row count와 hot user 분포를 확인한다. 구체적인 쿼리는 [Seed Data Guide](../../guides/seed-data.md)를 기준으로 한다.

## Phase 7 데이터 정리

같은 Docker volume에서 Phase 7을 실행한 뒤 Phase 1을 다시 측정한다면, 먼저 Phase 7 hot user 데이터와 pagination 인덱스를 제거한다.

```bash
docker compose exec -T postgres psql -U app -d ecommerce -v ON_ERROR_STOP=1 < scripts/phase-07/06-hot-user-cleanup.sql
```

정리 후 최소 확인값:

| 항목 | 기대값 |
|---|---:|
| `users` | 10,000 |
| `point_history` | 2,000,000 |
| `idx_point_history_created_id` | 없음 |
| `idx_point_history_user_created_id` | 없음 |

## Spring 서버 실행

Spring 서버는 `scripts/server.sh`로 실행한다.

```bash
./scripts/server.sh pool5
./scripts/server.sh pool10
./scripts/server.sh pool20
```

Hikari pool 값은 서버 재시작 후 적용된다. preset 상세는 [Spring Profiles Guide](../../guides/spring-profiles.md)를 기준으로 한다.

## k6 실행

k6는 `k6/run.sh`로 실행한다.

```bash
./k6/run.sh <scenario> <preset>
```

Prometheus remote write와 측정 조건 label이 필요하면 `prometheus` 모드를 사용한다.

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh <scenario> <preset> prometheus
```

scenario와 preset 상세는 [k6 Load Testing Guide](../../guides/k6-load-testing.md)를 기준으로 한다.

예시:

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders smoke prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
STRATEGY=baseline PHASE=phase-01 POOL=pool10 ./k6/run.sh products baseline prometheus
STRATEGY=baseline PHASE=phase-01 POOL=pool10 ./k6/run.sh products stress-100 prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh points phase1-points-page0 prometheus
```

ADR 0009 이후에도 Phase 1 측정 계약은 동일하다. `orders`는 `strategy=lazy`, `products`는 `strategy=baseline`, `points`는 Offset/Page endpoint를 사용한다. Product API 기본값은 이후 Phase의 `querydsl`이므로 Phase 1 상품 측정은 `STRATEGY=baseline`을 명시한다.

주의: `k6/presets/points-page0.json`은 Phase 7 hot user `707000` 조건으로 쓰인다. Phase 7 cleanup 이후 Phase 1의 points page0 기준선을 재측정할 때는 `phase1-points-page0` preset을 사용한다. `phase1-points-page500`은 deep page 선택 재측정용이며, 현재 Phase 1 결론의 필수 조건은 아니다.

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
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus

docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
STRATEGY=baseline PHASE=phase-01 POOL=pool10 ./k6/run.sh products baseline prometheus

docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
PHASE=phase-01 POOL=pool10 ./k6/run.sh points phase1-points-page0 prometheus
```

병목이 보이는 시나리오만 `pool5`, `pool20`으로 반복한다.

## 결과 저장 규칙

결과는 시나리오와 설정이 섞이지 않게 저장한다.

```text
docs/evidence/phase-01/
  clean-rerun-YYYY-MM-DD/
    README.md
    db/
      phase7-hot-user-cleanup.txt
      row-counts-after-cleanup.txt
      indexes-after-cleanup.txt
      row-counts-after-rerun.txt
      indexes-after-rerun.txt
    api-smoke/
      spring-health.json
      products-baseline.json
      orders-lazy-user950.json
      points-page0-user5714.json
      points-page500-user5714.json
    explain/
      products-baseline-filter.txt
      orders-lazy-and-order-item.txt
      points-page0-page500-count.txt
    k6/
      products-baseline/
        k6-summary.json
        k6-exit-status.txt
        pg-stat-statements.txt
        run-window.json
      orders-baseline/
        k6-summary.json
        k6-exit-status.txt
        pg-stat-statements.txt
        run-window.json
      points-page0/
        k6-summary.json
        k6-exit-status.txt
        pg-stat-statements.txt
        run-window.json
      points-page500/
        k6-summary.json
        k6-exit-status.txt
        pg-stat-statements.txt
        run-window.json
```

Historical evidence는 이전 구조로 남아 있을 수 있다.

```text
docs/evidence/phase-01/
  orders/pool10-baseline/
  products/pool10-baseline/
  points/pool10-page0/
  points/pool10-page500/
```

개별 k6 run에서 파일을 고정하려면 다음 환경변수를 사용한다.

```bash
K6_SUMMARY_JSON_FILE=docs/evidence/phase-01/clean-rerun-YYYY-MM-DD/k6/products-baseline/k6-summary.json \
K6_RUN_WINDOW_FILE=docs/evidence/phase-01/clean-rerun-YYYY-MM-DD/k6/products-baseline/run-window.json \
K6_EXIT_STATUS_FILE=docs/evidence/phase-01/clean-rerun-YYYY-MM-DD/k6/products-baseline/k6-exit-status.txt \
STRATEGY=baseline PHASE=phase-01 POOL=pool10 ./k6/run.sh products baseline prometheus
```

사람이 읽는 k6 종료 요약을 별도 파일로 남기려면 같은 명령에 `K6_LOG_FILE=<path>`를 추가한다. 이 파일은 필수 evidence가 아니다.

`pg_stat_statements` snapshot은 `docs/evidence/phase-01/<scenario>/<condition>/pg-stat-statements.txt`에 분리해서 저장한다.

## 주의 사항

- `seed-loadtest`는 데이터가 크다. 로컬 메모리와 디스크 여유를 확인한다.
- Hikari pool을 바꾸려면 Spring 서버를 재시작한다.
- k6 preset은 서버 재시작 없이 바꿀 수 있다.
- Phase 7 cleanup 이후에는 `707000` hot user가 삭제되므로 Phase 1 point rerun에 `points-page0` preset을 쓰지 않는다.
- `phase1-points-page500`은 clean `loadtest` 분포에서 page500 요청을 선택 재측정할 때만 사용한다. 현재 Phase 1 보고서는 page0부터 운영 한계에 도달했다는 기준선을 사용한다.
