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

- Docker/PostgreSQL/Grafana 기본 실행은 [Environment Guide](../../guides/environment.md)를 따른다.
- 데이터 시딩 preset과 검증 쿼리는 [Seed Data Guide](../../guides/seed-data.md)를 따른다.
- Spring profile과 Hikari pool preset은 [Spring Profiles Guide](../../guides/spring-profiles.md)를 따른다.
- k6 runner와 preset 사용법은 [k6 Load Testing Guide](../../guides/k6-load-testing.md)를 따른다.
- 공통 Grafana 패널과 DB 지표는 [Grafana Observability Guide](../../guides/grafana-observability.md)를 따른다.

## 인프라 기동

프로젝트 루트에서 실행한다.

```bash
docker compose up -d
docker compose ps
```

Grafana/Prometheus 접근 정보는 [Environment Guide](../../guides/environment.md)를 기준으로 한다.

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

Grafana 증빙까지 남길 때는 `prometheus` 모드와 측정 조건 label을 함께 사용한다.

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh <scenario> <preset> prometheus
```

scenario와 preset 상세는 [k6 Load Testing Guide](../../guides/k6-load-testing.md)를 기준으로 한다. 공통 대시보드는 [Grafana Observability Guide](../../guides/grafana-observability.md)의 `DB Lab Overview`를 기준으로 한다.

예시:

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders smoke prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh products baseline prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh products stress-100 prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh points points-page0 prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh points points-page500 prometheus
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
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus

docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
PHASE=phase-01 POOL=pool10 ./k6/run.sh products baseline prometheus

docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
PHASE=phase-01 POOL=pool10 ./k6/run.sh points points-page0 prometheus

docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
PHASE=phase-01 POOL=pool10 ./k6/run.sh points points-page500 prometheus
```

병목이 보이는 시나리오만 `pool5`, `pool20`으로 반복한다.

## 결과 저장 규칙

결과는 시나리오와 설정이 섞이지 않게 저장한다.

```text
docs/evidence/phase-01/
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

`pg_stat_statements` snapshot 쿼리는 [Grafana Observability Guide](../../guides/grafana-observability.md)를 기준으로 한다.

## 주의 사항

- `seed-loadtest`는 데이터가 크다. 로컬 메모리와 디스크 여유를 확인한다.
- Hikari pool을 바꾸려면 Spring 서버를 재시작한다.
- k6 preset은 서버 재시작 없이 바꿀 수 있다.
- `points-page500`은 hot user에게 충분한 포인트 이력이 있어야 병목이 보인다.
- Grafana 지표가 보이지 않으면 Prometheus scrape target과 compose 서비스 활성화 상태를 먼저 확인한다.
