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

## k6 with Prometheus

k6 지표를 Prometheus와 Grafana에서 보려면 `prometheus` 모드로 실행한다.

```bash
./k6/run.sh orders baseline prometheus
./k6/run.sh products baseline prometheus
./k6/run.sh points points-page500 prometheus
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
