# Phase 1 Evidence

Phase 1은 나이브 구현의 Baseline을 확보하는 단계다. 이 디렉토리는 `loadtest` 데이터와 `pool10` 조건에서 실행한 k6, SQL, Prometheus, Grafana 증거를 보관한다.

## 요약 문서

- [BASELINE.md](./BASELINE.md): Phase 2, Phase 3, Phase 7에서 비교할 기준선 측정값
- [Phase 1 Report](../../phases/01-baseline/report.md): 측정값 해석과 다음 Phase 판단

## 측정 조건

| 항목 | 값 |
|---|---|
| Date | `2026-05-17` |
| Seed preset | `loadtest` |
| Spring profile | `pool10` |
| k6 label | `phase=phase-01`, `pool=pool10` |
| Dashboard | `DB Lab Overview` |

## Scenario Evidence

| Scenario | Condition | 주요 파일 |
|---|---|---|
| `orders` | `pool10-baseline` | [k6 summary](./orders/pool10-baseline/k6-summary.txt), [pg_stat_statements](./orders/pool10-baseline/pg-stat-statements.txt), [screenshot](./grafana-screenshots/orders-baseline.png) |
| `products` | `pool10-baseline` | [k6 summary](./products/pool10-baseline/k6-summary.txt), [pg_stat_statements](./products/pool10-baseline/pg-stat-statements.txt), [seq scan](./products/pool10-baseline/product-seq-scan.json), [screenshot](./grafana-screenshots/products-baseline.png) |
| `points` | `pool10-page0` | [k6 summary](./points/pool10-page0/k6-summary.txt), [pg_stat_statements](./points/pool10-page0/pg-stat-statements.txt), [screenshot](./grafana-screenshots/points-page0.png) |
| `points` | `pool10-page500` | [k6 summary](./points/pool10-page500/k6-summary.txt), [pg_stat_statements](./points/pool10-page500/pg-stat-statements.txt), [screenshot](./grafana-screenshots/points-page500.png) |

## Grafana Screenshots

| 파일 | 의미 |
|---|---|
| [orders-baseline.png](./grafana-screenshots/orders-baseline.png) | 주문 목록 baseline 부하 중 Grafana 상태 |
| [products-baseline.png](./grafana-screenshots/products-baseline.png) | 상품 검색 baseline 부하 중 Grafana 상태 |
| [points-page0.png](./grafana-screenshots/points-page0.png) | 포인트 내역 page0 부하 중 Grafana 상태 |
| [points-page500.png](./grafana-screenshots/points-page500.png) | 포인트 내역 page500 부하 중 Grafana 상태 |

## 비교 기준

| 다음 Phase | 비교할 Phase 1 Evidence |
|---|---|
| Phase 2 Indexes | `products/pool10-baseline/` |
| Phase 3 N+1 | `orders/pool10-baseline/` |
| Phase 7 Pagination | `points/pool10-page0/`, `points/pool10-page500/` |
