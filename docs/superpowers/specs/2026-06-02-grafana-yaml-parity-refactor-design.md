# Grafana YAML Parity Refactor Design

## 배경

현재 Grafana dashboard는 `scripts/generate-db-lab-dashboard.mjs` 하나에서 생성된다.

이 파일은 다음 책임을 모두 함께 가진다.

- Grafana dashboard JSON 공통 구조 생성
- Prometheus datasource, variable, target, panel helper 정의
- k6, Spring, HikariCP, PostgreSQL PromQL 정의
- `DB Lab Overview` row/panel layout 구성
- Phase focus row 구성
- `docker/grafana/dashboards/db-lab-overview.json` 출력

현재 구조는 빠르게 dashboard를 만들기에는 단순하지만, phase가 늘어날수록 PromQL 수정, row 추가, layout 변경이 한 파일에 집중된다. 특히 Phase 6 aggregation, Phase 7 pagination 이후에는 dashboard가 learning phase evidence를 비교하는 핵심 관측 도구가 되었기 때문에, dashboard source of truth를 사람이 읽고 수정하기 쉬운 구조로 나눌 필요가 있다.

기존 root 참조 문서 `2026-06-01-grafana-dashboard-generator-yaml-refactor-design.md`는 `concurrency-lab-overview`와 reservation 중심 구조를 기준으로 작성되어 있다. 현재 repo는 `DB Lab Overview`와 ecommerce DB lab 구조를 사용한다. 따라서 해당 문서의 방향은 유지하되, 현재 프로젝트의 dashboard 계약에 맞춰 1단계 parity-first YAML 전환으로 진행한다.

## 목표

- `scripts/generate-db-lab-dashboard.mjs`의 monolith 구조를 YAML source + compiler 구조로 전환한다.
- 사람이 주로 수정하는 source of truth를 `scripts/grafana/**/*.yml`로 옮긴다.
- 생성 결과는 기존 `docker/grafana/dashboards/db-lab-overview.json` 계약을 유지한다.
- dashboard `uid`는 `db-lab-overview`를 유지한다.
- dashboard `title`은 `DB Lab Overview`를 유지한다.
- 기존 Grafana provisioning 경로를 유지한다.
- 기존 capture script가 기대하는 dashboard variable 계약을 유지한다.
- 기존 row/panel/PromQL/layout은 가능한 한 의미상 동일하게 유지한다.
- `make grafana-generate` 명령은 계속 동작한다.
- `scripts/verify-observability.mjs` 검증이 새 YAML generator 결과를 검증하도록 유지한다.

## 비목표

- 이번 단계에서 dashboard row/panel 구성을 개선하지 않는다.
- 이번 단계에서 Phase 6 focus row를 새로 설계하지 않는다.
- 이번 단계에서 Phase 7 focus row를 재배치하지 않는다.
- 이번 단계에서 PromQL 의미를 바꾸지 않는다.
- 이번 단계에서 capture script의 CLI 계약을 바꾸지 않는다.
- 이번 단계에서 Grafana UI에서 선택한 panel만 캡처하는 기능을 추가하지 않는다.
- 이번 단계에서 generated JSON을 사람이 직접 편집하는 방식으로 되돌리지 않는다.
- 이번 단계에서 evidence path, run-window, k6 실행 pipeline을 변경하지 않는다.

## 결정

1단계는 parity-first 전환으로 제한한다.

여기서 parity는 byte-for-byte 동일성을 뜻하지 않는다. JSON field order, panel id 부여 방식, formatting은 달라질 수 있다. 대신 다음 계약이 유지되어야 한다.

- dashboard uid/title/tags가 유지된다.
- dashboard variables가 유지된다.
- row title과 row 순서가 유지된다.
- panel title과 panel type이 유지된다.
- PromQL expression 의미가 유지된다.
- `zeroWhenNoData` fallback이 유지된다.
- Grafana provisioning output path가 유지된다.
- capture script가 기존 URL variable로 같은 dashboard를 열 수 있다.

즉, 이번 단계의 목표는 "더 좋은 dashboard"가 아니라 "기존 dashboard를 YAML source로 재생성하는 구조"다.

row/panel 개선은 YAML 구조가 안정된 뒤 2단계에서 별도 spec으로 진행한다.

## 목표 파일 구조

```text
scripts/
  generate-db-lab-dashboard.mjs

  grafana/
    generate.mjs

    dashboards/
      db-lab-overview.yml

    rows/
      run-summary.yml
      k6-load.yml
      spring-api.yml
      spring-runtime.yml
      hikari-pool.yml
      postgres-activity.yml
      table-access.yml
      phase-focus.yml

    queries/
      k6.yml
      spring.yml
      hikari.yml
      postgres.yml

    lib/
      yaml-loader.mjs
      query-registry.mjs
      dashboard-compiler.mjs
      grafana-builder.mjs
      layout.mjs
      write-dashboard.mjs
```

`scripts/generate-db-lab-dashboard.mjs`는 기존 명령 호환을 위해 남긴다. 실제 구현은 `scripts/grafana/generate.mjs`로 이동한다.

## Dashboard YAML

`scripts/grafana/dashboards/db-lab-overview.yml`은 dashboard metadata와 row 순서를 관리한다.

예상 구조:

```yaml
uid: db-lab-overview
title: DB Lab Overview
output: docker/grafana/dashboards/db-lab-overview.json
tags:
  - db-lab
  - ecommerce
  - observability

variables:
  phase:
    label: Phase
    query: label_values(k6_http_reqs_total, phase)
    default: phase-01
  scenario:
    label: Scenario
    query: label_values(k6_http_reqs_total{phase="$phase"}, scenario)
    default: orders
  preset:
    label: Preset
    query: label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario"}, preset)
    default: baseline
  pool:
    label: Pool
    query: label_values(k6_http_reqs_total{phase="$phase", scenario="$scenario", preset="$preset"}, pool)
    default: pool10
  uri:
    label: URI
    query: label_values(http_server_requests_seconds_count, uri)
    default: $__all
    includeAll: true
    multi: true
  table:
    label: Table
    query: label_values(pg_stat_user_tables_seq_scan, relname)
    default: $__all
    includeAll: true
    multi: true

rows:
  - run-summary
  - k6-load
  - spring-api
  - spring-runtime
  - hikari-pool
  - postgres-activity
  - table-access
  - phase-focus
```

`phase-focus`는 기존 JS generator의 phase focus row 목록을 표현한다. 이번 단계에서는 row 자체를 새로 설계하지 않고 기존 focus row들을 YAML로 옮긴다.

## Row YAML

`scripts/grafana/rows/*.yml`은 row와 panel 목록을 정의한다.

예상 구조:

```yaml
id: run-summary
title: Run Summary
panels:
  - type: stat
    title: k6 p95
    query: k6.http.p95
    unit: s
    layout:
      x: 0
      yOffset: 0
      w: 6
      h: 4

  - type: stat
    title: Actual RPS
    query: k6.http.rps
    unit: reqps
    layout:
      x: 12
      yOffset: 0
      w: 6
      h: 4
```

Parity-first 단계에서는 기존 layout을 최대한 유지해야 하므로, 필요한 panel에는 `layout` override를 허용한다.

향후 2단계에서는 layout을 더 자동화할 수 있지만, 이번 단계에서는 기존 screenshot/capture 안정성을 우선한다.

## Query YAML

`scripts/grafana/queries/*.yml`은 PromQL alias를 정의한다.

예상 구조:

```yaml
k6.http.p95:
  expr: histogram_quantile(0.95, sum(rate(k6_http_req_duration_seconds{phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"}[$__rate_interval])))
  zeroWhenNoData: true

k6.http.rps:
  expr: sum(rate(k6_http_reqs_total{phase="$phase", scenario="$scenario", preset="$preset", pool="$pool"}[$__rate_interval]))
  zeroWhenNoData: true
```

연결 규칙:

```text
rows/*.yml 의 panel.query == queries/*.yml 의 key
```

compiler는 모든 panel query alias가 존재하는지 검증해야 한다.

## Compiler 책임

### `yaml-loader.mjs`

- YAML 파일을 읽고 parse한다.
- 상대 경로는 repository root 기준으로 해석한다.
- YAML parse 실패 시 파일 경로를 포함한 명확한 error를 낸다.

### `query-registry.mjs`

- `scripts/grafana/queries/*.yml`을 읽어 query alias map을 만든다.
- 중복 alias가 있으면 fail fast 한다.
- row panel이 참조한 alias가 없으면 fail fast 한다.

### `grafana-builder.mjs`

- 기존 Grafana JSON panel 구조를 만드는 builder 함수를 제공한다.
- datasource는 기존처럼 `{ type: 'prometheus', uid: 'prometheus' }`를 유지한다.
- stat, timeseries, table panel 생성을 지원한다.
- target refId를 순서대로 부여한다.
- `zeroWhenNoData: true` query에는 no-data fallback을 적용한다.

### `layout.mjs`

- row와 panel의 grid position을 계산한다.
- parity-first 단계에서는 기존 layout을 보존하기 위해 explicit layout override를 우선한다.
- override가 없는 panel은 기본값을 사용한다.

### `dashboard-compiler.mjs`

- dashboard YAML, row YAML, query registry를 조합해 Grafana dashboard JSON을 만든다.
- variables를 Grafana templating list로 변환한다.
- row 순서대로 panels 배열을 만든다.
- dashboard uid/title/tags/schemaVersion/time/refresh 등 기존 공통 설정을 유지한다.

### `write-dashboard.mjs`

- output path의 상위 디렉터리를 만든다.
- generated JSON을 pretty JSON으로 쓴다.

## 기존 계약

다음 계약은 변경하지 않는다.

```text
Generated output:
  docker/grafana/dashboards/db-lab-overview.json

Dashboard:
  uid: db-lab-overview
  title: DB Lab Overview

Variables:
  phase
  scenario
  preset
  pool
  uri
  table

Make target:
  make grafana-generate

Capture script:
  node scripts/capture-grafana-dashboard.mjs --phase ... --scenario ... --preset ... --pool ...
```

## Dependency

YAML parsing을 위해 Node devDependency `yaml`을 추가한다.

```bash
npm install --save-dev yaml
```

`package.json`과 `package-lock.json`을 함께 갱신한다.

## 검증 전략

필수 검증:

```bash
make grafana-generate
rtk proxy node scripts/verify-observability.mjs
```

생성 JSON 검증:

- `docker/grafana/dashboards/db-lab-overview.json`이 존재한다.
- generated JSON을 `JSON.parse`로 읽을 수 있다.
- dashboard uid는 `db-lab-overview`다.
- dashboard title은 `DB Lab Overview`다.
- variables는 `phase`, `scenario`, `preset`, `pool`, `uri`, `table`을 포함한다.
- 기존 주요 row title이 유지된다.
- 모든 panel target은 refId를 가진다.
- no-data fallback이 필요한 query는 fallback을 가진다.
- `scripts/verify-observability.mjs`가 통과한다.

가능하면 다음도 확인한다.

```bash
docker compose ps grafana
npm run grafana:capture -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --pool pool10 --live
```

## 완료 기준

- YAML source가 `scripts/grafana/**/*.yml`에 존재한다.
- `scripts/grafana/lib/*.mjs` compiler 구조가 존재한다.
- `scripts/generate-db-lab-dashboard.mjs`는 새 generator를 호출하는 compatibility wrapper가 된다.
- `make grafana-generate`가 성공한다.
- generated `db-lab-overview.json`이 기존 dashboard 계약을 유지한다.
- `scripts/verify-observability.mjs`가 통과한다.
- capture script CLI 계약은 변경되지 않는다.
- row/panel 개선은 별도 2단계 작업으로 남는다.
