# Environment Guide

> Docker 기반 인프라를 기동하고, PostgreSQL/Prometheus/Grafana 접근 상태를 확인하는 공통 가이드다.

## Docker Compose

프로젝트 루트에서 실행한다.

```bash
docker compose up -d
docker compose ps
```

데이터를 완전히 초기화하려면 볼륨까지 삭제한다.

```bash
docker compose down -v
docker compose up -d
```

## Services

| Service | Default URL / Port | Purpose |
|---|---|---|
| PostgreSQL | `localhost:5432` | 실험 대상 DB |
| Prometheus | `http://localhost:9090` | Metric storage |
| Grafana | `http://localhost:3000` | Dashboard |
| postgres-exporter | `localhost:9187` | PostgreSQL metrics |

Prometheus는 k6 remote write를 받을 수 있게 `--web.enable-remote-write-receiver` 옵션으로 실행된다. Grafana는 `docker/grafana/provisioning`을 통해 Prometheus datasource를 자동 등록한다.

공통 대시보드는 `DB Lab / DB Lab Overview`를 기준으로 한다. 대시보드는 Spring Boot 서버가 실행되고 k6가 `prometheus` 모드로 한 번 이상 실행된 뒤에 의미 있는 값을 보여준다.

## Testcontainers

일부 Phase는 docker compose PostgreSQL volume 대신 PostgreSQL Testcontainers를 사용한다. 대표적으로 Phase 4 transaction isolation 테스트는 개발 DB 상태나 대량 seed 데이터에 의존하지 않고, test-only PostgreSQL 컨테이너에서 최소 fixture를 직접 reset한다.

```bash
cd ecommerce && rtk gradlew test --tests "*TransactionIsolationTest"
```

Testcontainers 실행에는 Docker daemon만 필요하다. `docker compose up -d`로 공통 PostgreSQL/Prometheus/Grafana stack을 띄울 필요는 없다. 자세한 기준은 [Testcontainers Integration Testing Guide](./testcontainers-integration-testing.md)를 따른다.

## k6 with Prometheus

k6 지표를 Prometheus와 Grafana에서 보려면 `prometheus` 모드로 실행한다.

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh products baseline prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh points points-page500 prometheus
```

이 모드는 `docker compose --profile test run --rm k6`를 사용하고, k6 결과를 `http://prometheus:9090/api/v1/write`로 remote write 한다.

## PostgreSQL Checks

```bash
docker compose exec postgres psql -U app -d ecommerce -c "SELECT version();"
docker compose exec postgres psql -U app -d ecommerce -c "SELECT * FROM pg_extension WHERE extname = 'pg_stat_statements';"
```

시나리오별 측정 전에 DB 통계를 초기화한다.

```bash
docker compose exec postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
docker compose exec postgres psql -U app -d ecommerce -c "VACUUM ANALYZE;"
```

## Result Directory

부하 테스트 결과는 시나리오와 설정이 섞이지 않게 저장한다.

```text
docs/evidence/
  phase-01/
    orders/
    products/
    points/
```
