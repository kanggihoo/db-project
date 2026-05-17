# k6 Load Testing Guide

> k6 runner, scenario, preset 사용법을 정리한다.

## Directory

```text
k6/
├── run.sh
├── orders-test.js
├── products-test.js
├── points-test.js
└── presets/
    ├── smoke.json
    ├── baseline.json
    ├── stress-100.json
    ├── stress-200.json
    ├── points-page0.json
    └── points-page500.json
```

| Path | Purpose |
|---|---|
| `k6/run.sh` | scenario와 preset을 받아 k6를 실행하는 runner |
| `k6/orders-test.js` | 주문 목록 API 부하 테스트 |
| `k6/products-test.js` | 상품 검색 API 부하 테스트 |
| `k6/points-test.js` | 포인트 내역 API 부하 테스트 |
| `k6/presets/*.json` | rate, duration, VU, page, user/category 범위 설정 |

## Run

```bash
./k6/run.sh <scenario> <preset> [local|prometheus]
```

`local` 모드는 로컬 `k6` 실행 파일이 있으면 로컬로 실행하고, 없으면 `grafana/k6` Docker 이미지를 사용한다.
`prometheus` 모드는 Docker Compose의 `k6` 서비스를 사용하고, k6 지표를 Prometheus remote write endpoint로 전송한다.

언제 실행하는가:

- Spring 서버가 떠 있고 API 정상 응답을 확인한 뒤
- 시나리오별 `pg_stat_statements_reset()`와 `VACUUM ANALYZE`를 실행한 뒤
- Phase별 기준선 또는 Before/After를 측정할 때

Grafana 증빙을 남길 때는 Measurement Condition을 label로 남긴다.

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
```

| Label | Example | Meaning |
|---|---|---|
| `phase` | `phase-01` | Learning Phase |
| `scenario` | `orders` | k6 scenario name |
| `preset` | `baseline` | k6 preset name |
| `pool` | `pool10` | Spring Hikari profile |

`scenario`와 `preset`은 `k6/run.sh` 인자에서 결정한다. `phase`와 `pool`은 환경변수로 전달한다. `userId`, `categoryId`, `page` 같은 요청별 값은 label로 남기지 않는다.

## Scenarios

| scenario | Target API | Purpose |
|---|---|---|
| `orders` | `GET /api/orders?userId=` | N+1, Hikari pool 점유 |
| `products` | `GET /api/products?categoryId=&status=` | 인덱스 없는 Seq Scan |
| `points` | `GET /api/points?userId=&page=&size=` | Offset deep page 병목 |

## Presets

| preset | rate | duration | Purpose |
|---|---:|---|---|
| `smoke` | 5 rps | 1m | API 정상 확인 |
| `baseline` | 50 rps | 5m | 기본 기준선 |
| `stress-100` | 100 rps | 5m | 부하 증가 |
| `stress-200` | 200 rps | 5m | 한계 확인 |
| `points-page0` | 50 rps | 5m | 얕은 페이지 |
| `points-page500` | 50 rps | 5m | 깊은 페이지 |

Preset files live in `k6/presets/`.

## Examples

```bash
./k6/run.sh orders smoke
./k6/run.sh orders baseline
./k6/run.sh products baseline
./k6/run.sh products stress-100
./k6/run.sh points points-page0
./k6/run.sh points points-page500
```

Grafana에서 k6 지표까지 함께 보려면 `prometheus` 모드를 사용한다.

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh products stress-100 prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh points points-page500 prometheus
```

## Adding a Preset

1. Add `k6/presets/<name>.json`.
2. Include `baseUrl`, `rate`, `duration`, `preAllocatedVUs`, `maxVUs`, and scenario-specific fields.
3. Run with `./k6/run.sh <scenario> <name>`.

## Adding a Scenario

1. Add `k6/<scenario>-test.js`.
2. Read `JSON.parse(open(__ENV.PRESET || 'presets/baseline.json'))`.
3. Keep executor settings driven by preset values.
4. Run with `./k6/run.sh <scenario> <preset>`.
