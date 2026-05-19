# Scripts Guide

> 프로젝트 루트의 `scripts/` 디렉토리에 있는 실행 스크립트를 설명한다.

## Directory

```text
scripts/
├── phase-02/
│   ├── 00-clean-product-indexes.sql
│   ├── 01-main-pre-index-explain.sql
│   ├── 02-create-main-index.sql
│   ├── 03-main-post-index-explain.sql
│   ├── 04-product-pg-stat-statements.sql
│   ├── 10-single-status-index.sql
│   ├── 20-composite-order-index.sql
│   ├── 30-covering-index.sql
│   └── 40-partial-index.sql
├── seed.sh
└── server.sh
```

## scripts/seed.sh

더미 데이터를 생성하는 Spring Boot seeder를 실행한다.

```bash
./scripts/seed.sh small
./scripts/seed.sh loadtest
```

| preset | 실행되는 Spring profiles | 목적 |
|---|---|---|
| `small` | `seeder,seed-small` | 빠른 로컬 확인 |
| `loadtest` | `seeder,seed-loadtest` | 부하 테스트 기준선 데이터 생성 |

언제 실행하는가:

- DB 볼륨을 처음 만든 뒤 스키마만 있고 데이터가 없을 때
- 데이터 규모나 hot user 분포를 바꿔 다시 실험할 때
- Phase 1 기준선 측정 전

주의 사항:

- `users` 테이블에 데이터가 있으면 seeder는 데이터를 다시 넣지 않는다.
- 같은 preset으로 다시 만들려면 `docker compose down -v`로 볼륨을 삭제한 뒤 실행한다.
- `loadtest`는 데이터가 크므로 로컬 메모리와 디스크 여유를 확인한다.

## scripts/server.sh

Spring Boot API 서버를 HikariCP pool preset으로 실행한다.

```bash
./scripts/server.sh pool5
./scripts/server.sh pool10
./scripts/server.sh pool20
```

| preset | 실행되는 Spring profile | 목적 |
|---|---|---|
| `pool5` | `pool5` | 작은 커넥션 풀에서 대기 병목 확인 |
| `pool10` | `pool10` | 기본 기준선 |
| `pool20` | `pool20` | pool 확장 효과 확인 |

언제 실행하는가:

- k6 부하 테스트 전에 API 서버를 띄울 때
- Hikari pool 크기별로 같은 k6 시나리오를 반복 비교할 때
- Phase 1 이후에도 DB 병목 실험을 할 때

주의 사항:

- Hikari pool 값은 서버 재시작 후 적용된다.
- pool 비교 실험은 같은 데이터, 같은 k6 preset, 같은 DB 통계 초기화 조건에서 반복한다.
- 서버 실행 중 다른 pool preset으로 바꾸려면 기존 서버를 종료하고 다시 실행한다.

## scripts/capture-grafana-dashboard.mjs

Captures the provisioned Grafana dashboard with Playwright and stitches the dashboard body into one long PNG.

Setup once:

```powershell
rtk npm install
rtk python -c "import PIL; print('ok')"
rtk docker compose up -d --force-recreate grafana
```

`docker-compose.yml` enables Grafana anonymous Viewer access, so this script opens dashboard URLs directly without login cookies.

Default capture:

```powershell
rtk npm run grafana:capture
```

The default output path is built from dashboard variables:

```text
docs/evidence/<phase>/grafana-screenshots/<scenario>-<preset>-<pool>.png
```

Examples:

```powershell
rtk npm run grafana:capture -- --phase phase-02 --scenario products --preset baseline --pool pool10
rtk npm run grafana:capture -- --phase phase-03 --scenario orders --preset baseline --pool pool10
```

The script also updates the dashboard URL variables. For example, `--phase phase-03` opens the dashboard with `var-phase=phase-03`, collapses all phase focus rows, expands only `Phase 3 N+1 Focus`, resets the internal Grafana scroll container to the top, captures clipped container images, then runs the Python stitch step.

Useful options:

| Option | Purpose |
|---|---|
| `--url <url>` | Start from a custom Grafana dashboard URL. Explicit variable options still override matching `var-*` parameters. |
| `--output <path>` | Override the final PNG path. |
| `--parts-dir <path>` | Override the temporary part capture directory. |
| `--phase <phase-id>` | Set `var-phase` and the phase focus row to expand. Supported: `phase-01`, `phase-02`, `phase-03`, `phase-07`. |
| `--scenario <name>` | Set `var-scenario`. |
| `--preset <name>` | Set `var-preset`. |
| `--pool <name>` | Set `var-pool`. |
| `--uri <pattern>` | Set `var-uri`. |
| `--table <pattern>` | Set `var-table`. |
| `--no-stitch` | Save only part captures and `capture-meta.json`. |
| `--no-align-phase-rows` | Keep the dashboard's current phase row state. |

Generated intermediate files are written under `docs/evidence/grafana-internal-scroll-captures/` and are ignored by git.
## scripts/phase-02/

Phase 2 인덱스 실험용 SQL 스크립트다. 운영 migration이 아니라 Learning Phase evidence를 반복 생성하기 위한 도구다.

주요 스크립트:

| Script | Purpose |
|---|---|
| `00-clean-product-indexes.sql` | product 실험 인덱스 정리 |
| `01-main-pre-index-explain.sql` | 메인 상품 검색 pre-index 실행계획 |
| `02-create-main-index.sql` | `idx_product_category_status` 생성 |
| `03-main-post-index-explain.sql` | 메인 상품 검색 post-index 실행계획 |
| `04-product-pg-stat-statements.sql` | product query snapshot |
| `10-single-status-index.sql` | 단일 status 인덱스 선택도 실험 |
| `20-composite-order-index.sql` | 복합 인덱스 순서 실험 |
| `30-covering-index.sql` | 커버링 인덱스 실험 |
| `40-partial-index.sql` | soft delete 부분 인덱스 실험 |

## Typical Flow

```bash
docker compose up -d
./scripts/seed.sh loadtest
./scripts/server.sh pool10
./k6/run.sh orders baseline
```

상세 시딩 설정은 [Seed Data Guide](./seed-data.md), Spring profile은 [Spring Profiles Guide](./spring-profiles.md), k6 실행은 [k6 Load Testing Guide](./k6-load-testing.md)를 기준으로 한다.
