# Makefile Command Interface Refactor Design

## 배경

이 프로젝트는 **Ecommerce DB Optimization Lab**의 Phase Evidence를 반복해서 만들기 위해 루트 `Makefile`을 공식 명령 인터페이스로 사용한다. 현재 `Makefile`은 작지만 다음 책임을 한 파일에 함께 가진다.

- 실행 환경 확인
- Docker Compose 기반 PostgreSQL, Prometheus, Grafana 실행
- Grafana dashboard JSON 생성
- Phase SQL 실행
- k6 evidence 실행
- Grafana dashboard 캡처
- k6 실행과 Grafana 캡처를 묶은 evidence capture
- phase evidence 상태 확인

이 구조는 Phase 6 aggregation 작업을 빠르게 진행하기에는 충분했다. 그러나 k6 runtime helper 분리와 Grafana YAML dashboard 관리가 추가되면서, Makefile도 같은 방향으로 경계를 명확히 해야 한다. 루트 `Makefile`은 사용자가 처음 만나는 얇은 진입점으로 두고, 실제 target 구현은 작업 종류별 `makefiles/*.mk`로 나눈다.

기존 참고 문서인 `2026-06-01-makefile-command-interface-refactor-design.md`의 핵심 방향은 유지한다. 다만 해당 문서는 Phase 3/4의 `phase3-*`, `phase4-*` compatibility target과 `03-db-strategies/...` 형태의 evidence path를 전제로 한다. 현재 프로젝트의 공개 명령은 이미 `phase-sql`, `k6-evidence`, `grafana-capture`, `evidence-capture`처럼 공통 target 중심이고, 내부 구현도 `npm run ...` Node wrapper 중심이다. 따라서 이번 spec은 현재 프로젝트의 Phase 6/7 명령 계약에 맞춘다.

## 현재 문제

### 1. 루트 Makefile의 책임이 한 파일에 모여 있다

현재 `Makefile`은 길지는 않지만 환경, DB, Grafana, SQL, k6, evidence orchestration, help 출력이 모두 같은 파일에 있다. target이 더 추가되면 사용자가 봐야 할 공개 인터페이스와 구현 세부사항이 섞이기 쉽다.

### 2. `PRESET`의 의미가 앞으로 갈라질 수 있다

현재 프로젝트에서는 `PRESET` 하나가 k6 preset과 Grafana dashboard variable에 같이 쓰인다. 지금은 이 계약이 단순하고 문서와도 맞다.

다만 k6 preset 파일명과 Grafana `preset` label이 나중에 달라질 가능성이 있다. 예를 들어 k6 runtime은 `review-summary-baseline`을 읽지만, Grafana 비교 label은 `baseline`으로 유지하고 싶을 수 있다. 이 가능성 때문에 내부 변수는 `K6_PRESET`과 `GRAFANA_PRESET`으로 분리 가능한 형태를 준비한다.

### 3. 참고 spec의 compatibility wrapper는 현재 프로젝트에 그대로 필요하지 않다

이전 프로젝트에서는 `makefiles/phase-compat.mk`에 다음과 같은 target을 두었다.

```text
phase3-grafana-capture
phase3-grafana-captures
phase3-grafana-stitch
phase3-grafana-stitches
phase3-sql-consistency
phase3-sql-consistencies
phase4-sql-consistency
```

이 target들은 과거에 이미 공개된 phase 전용 명령을 공통 target으로 위임하기 위한 compatibility layer였다.

현재 프로젝트의 루트 Makefile에는 위와 같은 Phase 3/4 공개 target이 없다. 따라서 이번 리팩토링에서는 `phase-compat.mk`를 만들지 않는다. 현재 공식 명령인 `phase-sql`은 compatibility wrapper가 아니라 Phase SQL runner의 공개 target이므로 `makefiles/sql.mk`에 둔다.

### 4. help 출력과 문서가 target 구현 경계와 함께 정리되어야 한다

`make help`와 `docs/guides/commands.md`는 사용자가 보는 공식 명령 표면이다. Makefile 파일 구조를 나누더라도 사용자는 기존처럼 루트에서 `make help`를 실행하고 같은 target을 사용할 수 있어야 한다.

## 목표

- 루트 `Makefile`을 shell 설정, 기본 goal, include 선언 중심의 얇은 진입점으로 축소한다.
- target 구현을 작업 종류별 `makefiles/*.mk`로 분리한다.
- 현재 공개 target 이름과 실행 계약을 유지한다.
- `PRESET` public 변수는 유지한다.
- 내부 파생 변수로 `K6_PRESET ?= $(PRESET)`과 `GRAFANA_PRESET ?= $(PRESET)`을 둔다.
- `k6-evidence`와 `evidence-capture`는 k6 실행에 `K6_PRESET`을 사용한다.
- `grafana-capture`는 Grafana dashboard variable에 `GRAFANA_PRESET`을 사용한다.
- `make help`는 공통 target과 핵심 변수 중심으로 유지한다.
- `docs/guides/commands.md`와 필요한 범위의 command guide를 새 구조에 맞게 갱신한다.
- 기존 Node wrapper인 `npm run k6:evidence`, `npm run grafana:capture`, `npm run phase:sql` 계약은 바꾸지 않는다.

## 비목표

- 이번 변경에서 k6 preset JSON 포맷을 바꾸지 않는다.
- 이번 변경에서 k6 scenario entrypoint나 `k6/lib/` 구조를 바꾸지 않는다.
- 이번 변경에서 Grafana dashboard YAML 구조를 바꾸지 않는다.
- 이번 변경에서 `scripts/run-k6-evidence.mjs`, `scripts/capture-grafana-dashboard.mjs`, `scripts/run-phase-sql.mjs` CLI 계약을 바꾸지 않는다.
- 이번 변경에서 Phase 3/4 compatibility target을 새로 추가하지 않는다.
- 이번 변경에서 Phase 5/6/7 evidence directory 구조를 이동하지 않는다.
- 이번 변경에서 모든 phase runbook의 과거 명령 예시를 한 번에 rewrite하지 않는다.

## 결정

Makefile command interface는 "루트 thin entrypoint + 작업 종류별 include 파일 + 현재 명령 호환" 구조로 리팩토링한다.

목표 구조는 다음과 같다.

```text
Makefile
makefiles/
  config.mk
  help.mk
  env.mk
  db.mk
  grafana.mk
  k6.mk
  evidence.mk
  sql.mk
```

`phase-compat.mk`는 이번 단계에서 만들지 않는다. 앞으로 실제 공개 legacy phase target을 유지해야 할 필요가 생기면 그때 추가한다.

## 파일별 책임

| File | Responsibility |
|---|---|
| `Makefile` | shell 설정, `.DEFAULT_GOAL`, include 순서 |
| `makefiles/config.mk` | 공통 변수 기본값, 파생 변수, 공통 argument fragment |
| `makefiles/help.mk` | `help` target 출력 |
| `makefiles/env.mk` | `env-check` |
| `makefiles/db.mk` | `db-start`, `db-shell` |
| `makefiles/grafana.mk` | `grafana-generate`, `grafana-capture` |
| `makefiles/k6.mk` | `k6-evidence` |
| `makefiles/evidence.mk` | `evidence-capture`, `phase-status` |
| `makefiles/sql.mk` | `phase-sql` |

## Target 정책

현재 공개 target은 유지한다.

```text
help
env-check
db-start
db-shell
grafana-generate
phase-sql
k6-evidence
grafana-capture
evidence-capture
phase-status
```

이번 단계에서 새로 만들지 않을 target은 다음과 같다.

```text
phase3-grafana-capture
phase3-sql-consistency
phase4-sql-consistency
phase5-*
phase6-*
phase7-*
```

phase별 편의 명령이 필요하더라도 먼저 공통 target과 변수 조합으로 표현한다. compatibility target은 이미 문서나 사용 흐름에 공개된 과거 명령을 유지해야 할 때만 추가한다.

## 변수 계약

### Public 변수

| Variable | Meaning | Default |
|---|---|---|
| `PHASE` | evidence와 dashboard phase id | `phase-06` |
| `SCENARIO` | SQL, k6, Grafana scenario | `review-summary` |
| `PRESET` | 기존 공개 preset 변수 | `review-summary-baseline` |
| `MODE` | k6 실행 모드 | `prometheus` |
| `POOL` | connection pool label | `pool10` |
| `PROFILE` | local profile | `local` |
| `CONDITION` | Measurement Condition 이름 | `naive-index` |
| `ACTION` | SQL runner action | `explain` |
| `TABLE` | Grafana table variable | empty |
| `OUTPUT` | 명시적 출력 경로 | empty |
| `WINDOW_FILE` | Grafana fixed capture window file | empty |
| `TAIL` | log tail 줄 수 | `120` |

### 내부 파생 변수

```make
K6_PRESET ?= $(PRESET)
GRAFANA_PRESET ?= $(PRESET)
```

기본 사용자는 계속 `PRESET`만 사용한다. k6 preset과 Grafana preset label을 명시적으로 분리해야 하는 경우에만 다음처럼 쓴다.

```bash
make k6-evidence \
  PHASE=phase-06 \
  SCENARIO=review-summary \
  K6_PRESET=review-summary-baseline \
  GRAFANA_PRESET=baseline \
  CONDITION=naive-index
```

`k6-evidence`는 `K6_PRESET`만 사용한다. `grafana-capture`는 `GRAFANA_PRESET`만 사용한다. `evidence-capture`는 Node wrapper 하나가 k6 실행과 Grafana 캡처를 함께 수행하므로 우선 `K6_PRESET`을 `--preset`으로 전달한다. 이후 Node wrapper가 preset 분리 계약을 지원하게 되면 `GRAFANA_PRESET` 전달을 별도 설계한다.

## 공통 Argument Fragment

현재 Makefile의 조건부 argument fragment는 `makefiles/config.mk`로 이동한다.

```make
PHASE_SQL_CONDITION_ARG = $(if $(filter data-profile,$(SCENARIO)),,--condition $(CONDITION))
GRAFANA_PHASE_ALIGN_ARG = $(if $(filter phase-06,$(PHASE)),--no-align-phase-rows,)
```

이 fragment들은 target 구현 파일에서 재사용한다.

## Target 설계

### `phase-sql`

목적: Phase SQL runner를 실행한다.

```bash
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/naive-index/explain.txt
```

실행 형태:

```make
npm run phase:sql -- --phase $(PHASE) --scenario $(SCENARIO) $(PHASE_SQL_CONDITION_ARG) --action $(ACTION) $(if $(OUTPUT),--output $(OUTPUT),)
```

### `k6-evidence`

목적: evidence 저장 규칙으로 k6 scenario를 실행한다.

```bash
make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index
```

실행 형태:

```make
npm run k6:evidence -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(K6_PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION)
```

### `grafana-capture`

목적: Grafana dashboard를 지정된 phase/scenario/preset/pool/window 조건으로 캡처한다.

```bash
make grafana-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index TABLE=review
```

실행 형태:

```make
npm run grafana:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(GRAFANA_PRESET) --pool $(POOL) $(if $(TABLE),--table $(TABLE),) $(if $(WINDOW_FILE),--window-file $(WINDOW_FILE),) $(if $(OUTPUT),--output $(OUTPUT),) $(GRAFANA_PHASE_ALIGN_ARG)
```

### `evidence-capture`

목적: k6 evidence 실행 후 Grafana capture까지 수행한다.

```bash
make evidence-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index TABLE=review
```

실행 형태:

```make
npm run evidence:capture -- --phase $(PHASE) --scenario $(SCENARIO) --preset $(K6_PRESET) --pool $(POOL) --mode $(MODE) --condition $(CONDITION) $(if $(TABLE),--table $(TABLE),) $(if $(OUTPUT),--output $(OUTPUT),)
```

## Help 출력 정책

`make help`는 파일이 나뉘어도 사용자가 보는 첫 화면으로 유지한다.

출력 순서는 다음을 따른다.

1. 자주 쓰는 target 예시
2. target별 핵심 필수 변수
3. default output root
4. preset 분리 고급 사용법은 짧게 언급

Compatibility target 목록은 이번 범위에 없다.

## 문서 갱신 범위

최소 갱신 대상은 다음이다.

- `docs/guides/commands.md`
- `docs/guides/project-format-standard.md`

갱신 원칙:

- 루트 `Makefile`이 공식 명령 인터페이스라는 설명을 유지한다.
- 현재 프로젝트에서 지원하는 target만 문서화한다.
- `PRESET`은 기본 public 변수로 설명한다.
- `K6_PRESET`과 `GRAFANA_PRESET`은 고급 분리 옵션으로 설명한다.
- Phase 6/7 runbook의 모든 명령을 한 번에 바꾸지 않는다.

## 검증 전략

필수 검증:

```bash
rtk proxy make help
rtk proxy make env-check
rtk proxy make grafana-generate
rtk proxy node scripts/verify-observability.mjs
```

Dry-run 검증:

```bash
rtk proxy make -n phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
rtk proxy make -n k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index TABLE=review
rtk proxy make -n grafana-capture PHASE=phase-06 SCENARIO=review-summary GRAFANA_PRESET=baseline CONDITION=naive-index TABLE=review
rtk proxy make -n evidence-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index TABLE=review
```

확인할 핵심:

- 루트 `Makefile` include가 정상 동작한다.
- 기존 public target 이름이 유지된다.
- `k6-evidence`는 `--preset $(K6_PRESET)`을 사용한다.
- `grafana-capture`는 `--preset $(GRAFANA_PRESET)`을 사용한다.
- 기존 `PRESET=...` 사용 방식이 계속 동작한다.
- `phase-sql`의 `data-profile` condition 생략 규칙이 유지된다.
- `phase-06` Grafana capture의 `--no-align-phase-rows` 규칙이 유지된다.

## 마이그레이션 순서

1. 현재 Makefile 명령 출력과 dry-run 결과를 baseline으로 확인한다.
2. `makefiles/config.mk`를 만들고 변수 기본값과 argument fragment를 이동한다.
3. `makefiles/help.mk`, `env.mk`, `db.mk`, `grafana.mk`, `k6.mk`, `evidence.mk`, `sql.mk`를 만든다.
4. 루트 `Makefile`을 include 중심으로 축소한다.
5. `K6_PRESET`과 `GRAFANA_PRESET` 파생 변수를 도입한다.
6. `k6-evidence`, `grafana-capture`, `evidence-capture`의 preset 사용을 target 목적에 맞게 조정한다.
7. `make help`와 command guide 문서를 갱신한다.
8. Makefile dry-run과 저비용 검증 명령을 실행한다.

## 완료 기준

- 루트 `Makefile`은 include 중심의 얇은 진입점이다.
- target 구현은 `makefiles/*.mk`에 책임별로 분리되어 있다.
- 현재 공개 target이 모두 유지된다.
- 기존 `PRESET` 기반 명령이 계속 동작한다.
- `K6_PRESET`과 `GRAFANA_PRESET`으로 preset 분리를 표현할 수 있다.
- `make help`, `make grafana-generate`, observability verifier가 통과한다.
- dry-run에서 핵심 target의 명령 확장이 의도대로 보인다.
