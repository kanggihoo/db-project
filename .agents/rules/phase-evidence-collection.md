---
trigger: manual
---

# Phase Evidence 수집 규칙

Learning Phase 증거는 측정 결과만 저장하지 않고, 같은 조건으로 재현 가능한 실행 조건과 재생성 경로도 함께 저장한다.

## 목적

각 증거 실행은 자신이 어떤 조건에서 실행됐고 어떤 명령으로 다시 만들 수 있는지 독립적으로 설명해야 한다.

이 규칙의 목적은 다음과 같다.

- Phase 1 baseline과 후속 Phase의 전/후 비교 조건을 명확히 고정한다.
- `docs/superpowers/plans/`의 실행 계획과 실제 증거 실행을 구분한다.
- 누락된 phase, scenario, preset, pool, endpoint, strategy, seed, DB state를 자동 검증할 수 있게 한다.
- k6 threshold 실패 같은 나쁜 결과도 유효한 Phase 증거로 남긴다.
- `EXPLAIN (ANALYZE, BUFFERS)`와 SQL snapshot을 사람이 수동으로 재작성하지 않고 재생성할 수 있게 한다.

## 기준 문서

공통 강제 규칙은 이 파일을 따른다.

Phase별 실행 절차는 `docs/phases/<phase>/runbook.md`에 둔다.

Phase별 관측 지표와 해석 기준은 `docs/phases/<phase>/observability.md`에 둔다.

실제 수집된 원본 파일 목록은 `docs/evidence/<phase>/README.md`에 둔다.

결과 해석과 다음 Phase 판단은 `docs/phases/<phase>/report.md`에 둔다.

`docs/superpowers/plans/`는 사람이 작업을 진행하기 위한 계획이다. 여기에 명령어 예시가 있을 수 있지만, 실제 실행 결과를 증명하는 산출물은 아니다.

## k6 증거 산출물

모든 k6 증거 실행 디렉토리는 `measurement.json`을 포함해야 한다.

예시 위치:

```text
docs/evidence/phase-01/.../k6/orders-baseline/
  measurement.json
  k6-summary.json
  k6-exit-status.txt
  run-window.json
  pg-stat-statements.txt
```

`measurement.json`은 해석 문서가 아니다. 해석은 Phase `report.md`에 둔다. `measurement.json`은 실행 조건과 증거 파일 관계를 고정하는 기계가 읽을 수 있는 manifest다. p95, p99, request rate, failure rate 같은 측정값은 `k6-summary.json` 원본에 두고, `measurement.json`에 중복 저장하지 않는다.

## 계획과 실행 기록의 관계

`docs/phases/<phase>/runbook.md`는 현재 유효한 공식 재현 절차다.

`docs/evidence/<phase>/.../measurement.json`은 실제로 실행된 측정의 조건 기록이다.

따라서 `measurement.json`은 필요하면 plan 출처를 참조하되, plan markdown을 파싱해서 신뢰하지 않는다.

## 측정 형식

모든 `measurement.json`은 `schemaVersion`을 포함하고 camelCase key를 사용한다.

```json
{
  "phase": "phase-01",
  "scenario": "orders",
  "condition": "orders-baseline",
  "preset": "baseline",
  "pool": "pool10",
  "target": {
    "method": "GET",
    "endpoint": "/api/orders",
    "queryParams": {
      "userId": "1..1000",
      "strategy": "lazy"
    },
    "strategy": "lazy"
  },
  "workload": {
    "rate": 50,
    "duration": "5m",
    "timeout": "5s",
    "preAllocatedVUs": 100,
    "maxVUs": 300
  },
  "execution": {
    "command": "PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus",
    "exitStatus": 99,
    "resultStatus": "threshold_failed"
  }
}
```

## 필수 필드

모든 k6 measurement는 아래 필드를 포함해야 한다.

- `phase`
- `scenario`
- `condition`
- `preset`
- `pool`
- `target.method`
- `target.endpoint`
- `target.queryParams`
- `workload.rate`
- `workload.duration`
- `workload.timeout`
- `evidence.exitStatus`
- `execution.command`
- `execution.exitStatus`
- `execution.resultStatus`

모든 유효한 measurement가 superpowers plan에서 만들어지는 것은 아니므로 `source.plan`은 선택 항목이다.

## 결과 상태

`execution.resultStatus`는 아래 고정 값을 사용한다.

| 값                 | 의미                                                                                            |
| ------------------ | ----------------------------------------------------------------------------------------------- |
| `passed`           | k6가 완료됐고 threshold를 통과했다.                                                             |
| `threshold_failed` | k6는 완료됐지만 하나 이상의 threshold가 실패했다. naive/baseline Phase에서는 유효한 evidence다. |
| `execution_failed` | 실행 자체가 실패했다. 원인을 조사하기 전에는 성능 evidence로 사용하지 않는다.                   |

k6 exit code `99`는 `threshold_failed`로 해석한다. threshold를 제거하거나 exit `0`으로 강제로 바꿔 이 실패를 숨기지 않는다.

## k6 요약 지표 규칙

Phase report 표의 실행 단위 지연 시간과 부하 값은 `k6-summary.json`을 기준 원본으로 사용한다.

Prometheus/Grafana 값은 실행 중 시간 흐름을 설명하는 시계열 증거다. `k6-summary.json`을 사용할 수 없는 이유를 report에 명시한 경우가 아니라면, 최종 표의 기준 원본으로 사용하지 않는다.

k6 summary를 생성하는 구현 세부사항은 `k6/run.sh`가 담당하며, 사용법은 `docs/guides/k6-load-testing.md`에 둔다.

## k6 evidence 실행 규칙

k6 evidence는 가능하면 `scripts/run-k6-evidence.mjs` 또는 Makefile target으로 실행한다.

이 wrapper는 evidence 저장 위치와 공통 파일 이름을 정한다.

- `k6-summary.json`
- `run-window.json`
- `k6-exit-status.txt`
- `measurement.json`

또한 wrapper는 실행 인자에서 알 수 있는 값을 `measurement.json`에 기록한다.

- `phase`
- `scenario`
- `condition`
- `preset`
- `pool`
- preset 파일의 rate, duration, timeout 같은 workload 값
- k6 exit code와 result status
- 실행 명령

반대로 wrapper가 자동으로 알 수 없는 값은 실행자가 명시해야 한다.

- `environment.seed`
- `environment.dbState`
- `source.plan`
- endpoint별 strategy나 query parameter 범위처럼 preset만으로 설명되지 않는 조건

`k6/run.sh`는 실제 k6 실행을 담당한다. evidence 디렉토리와 파일 경로 결정은 `scripts/run-k6-evidence.mjs`가 담당한다.

## SQL / EXPLAIN 증거 규칙

Phase report나 evidence README에서 SQL snapshot, `EXPLAIN`, `EXPLAIN (ANALYZE, BUFFERS)` 결과를 인용한다면, 해당 결과를 재생성하는 SQL 파일이 저장소에 있어야 한다.

필수 규칙:

- SQL 파일은 `scripts/phase-XX/*.sql`에 둔다.
- 파일명은 실행 순서와 목적을 드러낸다.
- 실행계획을 남길 때는 `EXPLAIN`과 `EXPLAIN (ANALYZE, BUFFERS)`를 둘 다 남긴다.
- 두 결과는 하나의 SQL 파일에서 `\echo`로 구분해서 남길것
- 실행계획을 인용하는 최종 evidence는 어떤 SQL 파일로 만들었는지 `runbook.md`에서 확인 가능해야 한다.
- 최종 evidence를 사람이 직접 `docker compose exec ... psql`로만 수동 실행하고 SQL 파일을 남기지 않으면 재현성 누락으로 본다.
- prepare SQL이 데이터, 인덱스, constraint처럼 planner 판단에 영향을 줄 수 있는 상태를 바꾼다면, 측정 전에 관련 테이블에 대해 `VACUUM ANALYZE <table>;`를 명시적으로 실행한다.

예시 파일명:

```text
scripts/phase-01/10-products-baseline-explain.sql
scripts/phase-01/20-orders-lazy-explain.sql
scripts/phase-01/30-points-offset-explain.sql
```

공통 실행기는 `scripts/run-phase-sql.mjs`를 사용한다. 이 wrapper는 phase별 mapping을 갖지 않고, 명시된 SQL 파일만 실행한다.

표준 실행 형식:

```bash
make phase-sql FILE=scripts/phase-01/10-products-baseline-explain.sql OUTPUT=docs/evidence/phase-01/clean-rerun-YYYY-MM-DD/explain/products-baseline-filter.txt
```

또는 직접 실행한다.

```bash
npm run phase:sql -- --file scripts/phase-01/10-products-baseline-explain.sql --output docs/evidence/phase-01/clean-rerun-YYYY-MM-DD/explain/products-baseline-filter.txt
```

## DB 상태와 복구 규칙

Phase evidence 수집 전에는 현재 DB 상태가 seed baseline과 같은지 확인해야 한다.

seed baseline은 `scripts/seed.sh <preset>` 직후의 공통 기준 상태다. seed baseline의 snapshot은 여러 Phase가 공유할 수 있는 evidence로 둔다.

예시 위치:

```text
docs/evidence/common/seed-loadtest/seed-state.txt
```

표준 생성 명령:

```bash
make seed-state SEED_PRESET=loadtest
```

이 명령은 `scripts/seed.sh loadtest`를 실행한 뒤 `scripts/db-state/00-seed-state.sql`로 seed baseline snapshot을 저장한다.

각 Phase는 seed baseline 자체를 다시 정의하지 않는다. 각 Phase는 측정 전에 현재 DB가 seed baseline과 같은지 확인하고, Phase 실험에 필요한 변경이 있다면 저장소에 남긴 SQL로 변경한 뒤 다시 확인한다.

각 Phase는 테스트 대상 테이블의 기본 통계를 evidence로 남겨야 한다. 최소 항목은 row count, 관련 index 목록, 관련 constraint 목록, Phase 조건에 필요한 분포 통계다.

필수 흐름:

1. Phase 측정 전 DB 상태 확인을 실행한다.
2. 현재 DB 상태가 seed baseline과 다르면 seed baseline으로 복구한다.
3. 복구 후 DB 상태 확인을 다시 실행해 seed baseline과 같은지 확인한다.
4. Phase 또는 condition에 필요한 prepare SQL을 실행한다.
5. prepare가 데이터, 인덱스, constraint를 바꿨다면 관련 테이블에 `VACUUM ANALYZE <table>;`를 실행한다.
6. prepare 후 DB 상태 확인을 실행해 현재 Phase expected state가 됐는지 확인한다.
7. k6, SQL/EXPLAIN, `pg_stat_statements`, Grafana/Prometheus evidence는 DB 상태 확인과 prepare 검증 이후에 수집한다.

seed baseline으로 복구하는 방법은 둘 중 하나다.

- fresh volume: `docker compose down -v`, `docker compose up -d`, `scripts/seed.sh <preset>`
- reset SQL: `scripts/db-reset/*.sql`

`scripts/db-reset/`는 공통 seed baseline으로 되돌리기 위한 reset SQL 저장소다. 이 디렉토리는 모든 reset SQL을 미리 만들어 두는 곳이 아니다. 새로운 Phase나 실험에서 seed baseline을 깨는 상태가 발견되면, 그 상태를 복구하는 SQL을 상황에 맞게 추가하고 저장소에 남긴다.

`scripts/db-reset/`에 둘 수 있는 예:

```text
scripts/db-reset/10-drop-known-phase-indexes.sql
scripts/db-reset/20-remove-known-test-fixtures.sql
scripts/db-reset/30-drop-temp-sample-tables.sql
```

`scripts/db-reset/`에 두지 않는 것:

- Phase 실험 조건을 만드는 prepare SQL
- Phase report에서 인용할 EXPLAIN SQL
- 특정 Phase에서만 의미 있는 성능 비교 SQL

이 파일들은 `scripts/phase-XX/`에 둔다.

reset SQL로 seed baseline 복구를 증명할 수 없으면 fresh volume과 seed를 사용한다. 예를 들어 seed 데이터가 대량 삭제됐거나 원본 값이 변경된 경우에는 reset SQL보다 fresh seed가 우선이다.

## Runbook 작성 규칙

Evidence 수집이 필요한 Phase의 `runbook.md`는 아래 항목을 재현 가능한 명령으로 포함해야 한다.

- seed baseline 또는 현재 DB 상태 확인 명령
- 재사용 볼륨을 쓰고 seed baseline과 다르면 `scripts/db-reset/*.sql` 실행 명령
- reset 후 seed baseline 복구 확인 명령
- 데이터 준비 또는 cleanup 명령
- 데이터, 인덱스, constraint 변경 후 필요한 `VACUUM ANALYZE <table>;` 명령
- prepare 후 Phase expected state 확인 명령
- Spring profile 또는 pool preset
- k6 실행 명령
- k6 evidence 저장 위치 또는 evidence wrapper 명령
- `pg_stat_statements` reset/snapshot 명령
- `EXPLAIN`과 `EXPLAIN (ANALYZE, BUFFERS)` evidence를 생성하는 `make phase-sql FILE=... OUTPUT=...` 명령
- Grafana/Prometheus capture가 주요 또는 보조 evidence라면 capture 명령과 time window 기준

`runbook.md`에는 공통 규칙을 길게 반복하지 않는다. 공통 규칙은 이 파일을 참조하고, Phase별 실제 명령만 둔다.

## Makefile Evidence Target 규칙

공통 Makefile evidence target은 특정 Phase 전용 기본값을 가지면 안 된다.

필수 규칙:

- `PHASE`, `SCENARIO`, `CONDITION`은 k6 evidence run에서 명시해야 한다.
- `PRESET` 또는 `K6_PRESET`은 k6 evidence run에서 명시해야 한다.
- `PRESET` 또는 `GRAFANA_PRESET`은 Grafana capture에서 명시해야 한다.
- `FILE`은 SQL evidence run에서 명시해야 한다.
- `POOL=pool10`, `MODE=prometheus`처럼 Phase와 무관한 운영 기본값은 허용한다.

잘못된 예:

```bash
make k6-evidence
```

올바른 예:

```bash
make k6-evidence PHASE=phase-01 SCENARIO=orders PRESET=baseline CONDITION=orders-baseline
```

## Measurement Contract 규칙

더 강한 검증이 필요하면 Phase는 아래와 같은 measurement contract 파일을 정의할 수 있다.

```text
docs/phases/<phase>/measurements.json
```

contract는 해당 Phase에서 허용되는 run을 선언한다. k6 evidence run의 `phase`, `scenario`, `condition`, `preset`, `pool`, `target.strategy`가 contract와 맞지 않으면 실패하거나 경고해야 한다.

## 리뷰 규칙

Phase 완료 여부를 검토할 때 아래 항목이 빠져 있으면 증거가 부족한 것으로 본다.

- k6 evidence run에 `measurement.json`이 없음
- k6 evidence run에 `run-window.json` 또는 `k6-exit-status.txt`가 없음
- measurement 시작 전에 seed baseline 또는 Phase expected state를 보여주는 DB state evidence가 없음
- 재사용 볼륨 reset이 필요했지만 대응되는 `scripts/db-reset/*.sql` evidence가 없음
- Phase prepare가 DB state를 변경했지만 저장소에 남긴 `scripts/phase-XX/*.sql` prepare 파일이 없음
- Phase prepare가 데이터, 인덱스, constraint를 변경했지만 관련 테이블 `VACUUM ANALYZE` 실행 근거가 없음
- Phase prepare 실행 후 DB state evidence가 없음
- `report.md`에서 인용한 SQL/EXPLAIN output에 대응되는 `scripts/phase-XX/*.sql` 원본 파일이 없음
- 실행계획 evidence에 `EXPLAIN` 또는 `EXPLAIN (ANALYZE, BUFFERS)` 중 하나만 있음
- `report.md`에서 인용한 SQL/EXPLAIN output에 대응되는 `runbook.md` 재생성 명령이 없음
- evidence 파일을 추가했지만 `docs/evidence/<phase>/README.md`를 갱신하지 않음
