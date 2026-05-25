# Project Format Standard

> 여러 Learning Phase 프로젝트에서 동일한 문서 구조와 실행 인터페이스를 사용하기 위한 표준이다.

## 목적

프로젝트마다 내부 구현, 프레임워크, 스크립트 파일명은 달라도 사람이 사용하는 구조와 명령은 같아야 한다.

이 표준의 목표는 다음 문제를 줄이는 것이다.

- 프로젝트마다 `scripts/` 파일명이 달라 실행법을 다시 찾아야 하는 문제
- phase별 문서와 evidence 위치가 달라 결과를 비교하기 어려운 문제
- k6, Grafana, Playwright, 이미지 후처리 같은 반복 작업의 명령 형식이 프로젝트마다 달라지는 문제
- AI agent가 매번 프로젝트별 `docs/guides/scripts.md`를 먼저 읽어야 하는 문제

## 기본 원칙

- 공식 실행 인터페이스는 루트 `Makefile`이다.
- `package.json` scripts는 Node 기반 내부 작업 레이어로 유지할 수 있다.
- `scripts/`는 실제 구현 파일을 둔다. 사용자는 반복 작업에서 직접 실행하지 않는다.
- phase 문서는 `docs/phases/`에서 시작한다.
- 실험 결과, 로그, 스크린샷은 `docs/evidence/`에 저장한다.
- 의사결정은 `docs/adr/`에 남긴다.
- Superpowers 산출물은 `docs/superpowers/specs/`, `docs/superpowers/plans/`에 둔다.

## 표준 디렉토리 구조

```text
.
├── Makefile
├── package.json
├── scripts/
│   ├── server/
│   ├── seed/
│   ├── load/
│   ├── observability/
│   ├── evidence/
│   └── phase-XX/
├── docs/
│   ├── README.md
│   ├── adr/
│   ├── agents/
│   ├── evidence/
│   │   ├── README.md
│   │   └── phase-XX/
│   │       ├── README.md
│   │       ├── grafana-screenshots/
│   │       └── <scenario>/
│   ├── guides/
│   │   ├── commands.md
│   │   ├── environment.md
│   │   ├── k6-load-testing.md
│   │   ├── grafana-observability.md
│   │   └── project-format-standard.md
│   ├── phases/
│   │   ├── README.md
│   │   └── XX-name/
│   │       ├── README.md
│   │       ├── scope.md
│   │       ├── runbook.md
│   │       ├── observability.md
│   │       └── report.md
│   └── superpowers/
│       ├── README.md
│       ├── specs/
│       └── plans/
```

프로젝트 특성상 없는 기능은 빈 디렉토리로 만들지 않는다. 단, 기능이 생기면 위 위치를 따른다.

## Makefile 계약

모든 프로젝트는 루트에서 `make help`를 지원해야 한다.

```bash
make help
```

`make help`는 최소한 다음 정보를 보여준다.

- 사용 가능한 target 목록
- target별 필수 변수
- 자주 쓰는 예시
- 결과물이 저장되는 기본 위치

## 공통 Make Target

아래 target 이름은 프로젝트 간 동일하게 유지한다.

| Target | Purpose | Required Variables | Common Optional Variables |
|---|---|---|---|
| `help` | 사용 가능한 명령 목록 출력 | - | - |
| `env-check` | 필수 도구와 실행 환경 확인 | - | - |
| `server-start` | 애플리케이션 서버 실행 | - | `PROFILE`, `POOL`, `PORT` |
| `server-stop` | 애플리케이션 서버 종료 | - | - |
| `server-logs` | 서버 로그 확인 | - | `TAIL` |
| `seed` | 실험용 데이터 생성 | - | `PRESET` |
| `db-start` | DB와 관측 도구 실행 | - | `PROFILE` |
| `db-reset` | 로컬 DB 초기화 | - | `VOLUME`, `CONFIRM` |
| `grafana-generate` | Grafana dashboard JSON 생성 | - | `DASHBOARD` |
| `grafana-capture` | Grafana dashboard 캡처 | - | `PHASE`, `SCENARIO`, `PRESET`, `POOL`, `TABLE`, `OUTPUT` |
| `k6-run` | k6 시나리오 실행 | `SCENARIO` | `PRESET`, `MODE`, `PHASE`, `POOL` |
| `k6-evidence` | evidence 저장 규칙으로 k6 실행 | `PHASE`, `SCENARIO`, `CONDITION` | `PRESET`, `POOL`, `MODE` |
| `evidence-capture` | k6 실행 후 Grafana 캡처까지 수행 | `PHASE`, `SCENARIO`, `CONDITION` | `PRESET`, `POOL`, `TABLE`, `OUTPUT` |
| `evidence-postprocess` | 캡처 이미지 후처리 | - | `INPUT`, `OUTPUT`, `PHASE` |
| `phase-status` | 현재 phase 문서와 evidence 상태 확인 | `PHASE` | - |

해당 프로젝트에 없는 기능은 target을 만들지 않는다. 다만 같은 기능을 제공한다면 위 이름을 사용한다.

## 표준 변수 이름

Makefile 변수명은 대문자 snake case를 사용한다.

| Variable | Meaning | Example |
|---|---|---|
| `PHASE` | phase id | `phase-02` |
| `SCENARIO` | k6 또는 실험 시나리오 | `products` |
| `PRESET` | 부하 테스트 preset | `baseline` |
| `MODE` | 실행 모드 | `local`, `prometheus` |
| `POOL` | connection pool preset | `pool10` |
| `PROFILE` | app profile | `local`, `test`, `pool10` |
| `CONDITION` | evidence 조건 이름 | `pool10-post-index` |
| `TABLE` | Grafana table variable | `product` |
| `DASHBOARD` | dashboard 이름 | `db-lab-overview` |
| `OUTPUT` | 명시적 출력 경로 | `docs/evidence/phase-02/grafana-screenshots/products.png` |
| `INPUT` | 명시적 입력 경로 | `docs/evidence/phase-02/raw.png` |
| `TAIL` | 로그 tail 줄 수 | `100` |

기본값은 Makefile 상단에 둔다.

```makefile
PHASE ?= phase-01
SCENARIO ?= orders
PRESET ?= baseline
MODE ?= prometheus
POOL ?= pool10
PROFILE ?= local
CONDITION ?= baseline
TAIL ?= 120
```

## Makefile 예시

```makefile
.PHONY: help server-start seed k6-run k6-evidence evidence-capture grafana-capture grafana-generate

PHASE ?= phase-01
SCENARIO ?= orders
PRESET ?= baseline
MODE ?= prometheus
POOL ?= pool10
PROFILE ?= local
CONDITION ?= baseline
TABLE ?=
OUTPUT ?=

help:
	@echo "Available targets:"
	@echo "  make server-start POOL=pool10"
	@echo "  make seed PRESET=loadtest"
	@echo "  make k6-run SCENARIO=orders PRESET=baseline MODE=local"
	@echo "  make k6-evidence PHASE=phase-02 SCENARIO=products CONDITION=pool10-post-index"
	@echo "  make evidence-capture PHASE=phase-02 SCENARIO=products CONDITION=pool10-post-index TABLE=product"
	@echo "  make grafana-capture PHASE=phase-02 SCENARIO=products POOL=pool10"

server-start:
	./scripts/server.sh $(POOL)

seed:
	./scripts/seed.sh $(PRESET)

k6-run:
	PHASE=$(PHASE) POOL=$(POOL) ./k6/run.sh $(SCENARIO) $(PRESET) $(MODE)

k6-evidence:
	npm run k6:evidence -- \
		--phase $(PHASE) \
		--scenario $(SCENARIO) \
		--preset $(PRESET) \
		--pool $(POOL) \
		--mode $(MODE) \
		--condition $(CONDITION)

evidence-capture:
	npm run evidence:capture -- \
		--phase $(PHASE) \
		--scenario $(SCENARIO) \
		--preset $(PRESET) \
		--pool $(POOL) \
		--mode $(MODE) \
		--condition $(CONDITION) \
		$(if $(TABLE),--table $(TABLE),) \
		$(if $(OUTPUT),--output $(OUTPUT),)

grafana-capture:
	npm run grafana:capture -- \
		--phase $(PHASE) \
		--scenario $(SCENARIO) \
		--preset $(PRESET) \
		--pool $(POOL) \
		$(if $(TABLE),--table $(TABLE),) \
		$(if $(OUTPUT),--output $(OUTPUT),)

grafana-generate:
	node scripts/generate-db-lab-dashboard.mjs
```

## package.json scripts 역할

`package.json` scripts는 제거하지 않는다. Node 기반 작업의 의미 있는 내부 이름으로 유지한다.

권장 역할 분리는 다음과 같다.

```text
Makefile
  사람이 사용하는 공식 실행 인터페이스

package.json scripts
  Node 기반 실행 단위
  Playwright, Grafana capture, k6 evidence wrapper 실행

scripts/
  실제 구현 파일
```

예시:

```json
{
  "scripts": {
    "k6:evidence": "node scripts/run-k6-evidence.mjs",
    "evidence:capture": "node scripts/run-k6-evidence.mjs --capture",
    "grafana:capture": "node scripts/capture-grafana-dashboard.mjs",
    "test:grafana-capture": "node --test scripts/grafana-capture-utils.test.mjs"
  }
}
```

문서와 README에서는 기본적으로 `make` 명령을 안내한다. `npm run ...`은 디버깅이나 Node task 개발 시에만 직접 안내한다.

## scripts 디렉토리 규칙

새 프로젝트는 기능별 하위 디렉토리를 사용한다.

```text
scripts/
  server/
    start.sh
    stop.sh
    logs.sh
  seed/
    run.sh
  load/
    k6-run.sh
    k6-evidence.mjs
  observability/
    grafana-generate.mjs
    grafana-capture.mjs
  evidence/
    postprocess-images.py
  phase-XX/
    00-prepare.sql
    10-measure.sql
```

기존 프로젝트는 즉시 파일을 이동하지 않아도 된다. 먼저 Makefile에서 기존 파일을 감싸고, phase 작업이 안정된 뒤 내부 파일명을 정리한다.

## docs/phases 규칙

각 phase는 아래 파일을 기본으로 둔다.

```text
docs/phases/XX-name/
  README.md
  scope.md
  runbook.md
  observability.md
  report.md
```

| File | Purpose |
|---|---|
| `README.md` | phase 목표, 상태, 핵심 링크 |
| `scope.md` | 이 phase에서 다루는 것과 제외하는 것 |
| `runbook.md` | 실행 순서와 재현 절차 |
| `observability.md` | 관측 지표, dashboard, 캡처 기준 |
| `report.md` | 결과 요약, 해석, 다음 phase로 넘길 내용 |

agent는 phase 작업을 시작할 때 모든 phase 디렉토리를 훑지 않는다. 먼저 대상 phase를 확정하고 다음 문서만 읽는다.

```text
docs/phases/README.md
docs/phases/<target-phase>/README.md
```

필요한 경우에만 해당 README가 연결한 추가 문서를 읽는다.

## docs/evidence 규칙

Evidence는 phase 단위로 저장한다.

```text
docs/evidence/
  README.md
  phase-XX/
    README.md
    grafana-screenshots/
      <scenario>-<condition>.png
    <scenario>/
      <condition>/
        k6-summary.txt
        run-window.json
        notes.md
```

권장 이름:

```text
docs/evidence/<phase>/<scenario>/<condition>/k6-summary.txt
docs/evidence/<phase>/<scenario>/<condition>/run-window.json
docs/evidence/<phase>/grafana-screenshots/<scenario>-<condition>.png
```

`condition`은 비교 가능한 실험 조건을 나타낸다.

예시:

```text
pool10-baseline
pool10-post-index
pool10-lazy
pool10-fetch-join
```

## docs/guides 규칙

`docs/guides/commands.md`는 Makefile target의 설명 문서다.

포함해야 할 내용:

- target별 목적
- 필수 변수와 기본값
- 대표 실행 예시
- 결과물 저장 위치
- 사전 조건

`docs/guides/scripts.md`는 내부 구현 스크립트 설명으로만 사용한다. 사람이 반복 작업을 실행하기 위해 먼저 읽어야 하는 문서가 되어서는 안 된다.

## docs/adr 규칙

반복 적용되는 구조, 실행 계약, evidence 저장 방식은 ADR로 남긴다.

ADR이 필요한 경우:

- Makefile을 공식 실행 인터페이스로 채택
- evidence 저장 위치를 `docs/evidence/`로 고정
- Grafana dashboard를 프로젝트당 하나로 통합
- phase 문서 구조를 고정
- Windows Git Bash, WSL2 같은 지원 환경 결정

ADR 형식:

```text
docs/adr/NNNN-title.md
```

## 지원 실행 환경

권장 지원 환경:

- macOS
- Linux
- Windows WSL2
- Windows Git Bash

Windows Git Bash는 다음 조건을 만족해야 한다.

- `make` 명령 사용 가능
- `bash` script 실행 가능
- `docker`, `node`, `npm`, `k6` 같은 필수 도구가 Git Bash에서 호출 가능
- Docker 컨테이너 내부 경로가 Git Bash 경로 변환에 의해 깨지지 않도록 필요한 스크립트에서 `MSYS_NO_PATHCONV=1` 처리

Windows PowerShell/CMD 네이티브 실행은 기본 지원 환경으로 보지 않는다.

## 새 프로젝트 적용 체크리스트

1. 루트에 `Makefile`을 만든다.
2. `make help`를 먼저 구현한다.
3. 기존 실행 스크립트를 Make target으로 감싼다.
4. `PHASE`, `SCENARIO`, `PRESET`, `MODE`, `POOL`, `CONDITION` 변수명을 표준에 맞춘다.
5. `docs/README.md`에 문서 구조를 정리한다.
6. `docs/guides/commands.md`를 만든다.
7. `docs/guides/scripts.md`는 내부 스크립트 설명으로 한정한다.
8. `docs/phases/README.md`와 대상 phase README를 만든다.
9. evidence 저장 위치를 `docs/evidence/<phase>/`로 맞춘다.
10. Superpowers spec과 plan을 `docs/superpowers/` 아래로 이동한다.
11. Makefile 공식 채택 여부를 ADR로 남긴다.

## 기존 프로젝트 마이그레이션 순서

기존 프로젝트는 한 번에 구조를 바꾸지 않는다.

1. 현재 실행 명령을 `docs/guides/scripts.md`에서 수집한다.
2. 명령을 공통 target 이름으로 매핑한다.
3. 루트 `Makefile`을 추가한다.
4. 기존 스크립트 파일은 이동하지 않고 Makefile에서 호출한다.
5. `docs/guides/commands.md`를 추가한다.
6. README와 phase runbook의 실행 예시를 `make ...` 형식으로 바꾼다.
7. 새 phase부터 표준 `scripts/` 하위 디렉토리 구조를 적용한다.
8. 기존 스크립트 이동은 별도 변경으로 처리한다.

## 금지 규칙

- phase별 실행 절차를 README 본문에만 숨기지 않는다.
- evidence 파일을 프로젝트 루트나 임시 디렉토리에 남기지 않는다.
- 반복 실행 명령을 `package.json`, shell script, README에 각각 다른 이름으로 중복 정의하지 않는다.
- 같은 기능의 Make target 이름을 프로젝트마다 다르게 만들지 않는다.
- agent가 매번 `scripts/` 파일명을 직접 찾아 실행해야 하는 구조로 두지 않는다.

