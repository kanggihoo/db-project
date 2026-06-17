# k6 Lib Refactor Design

## 배경

현재 k6 부하 테스트는 scenario별 entrypoint 파일로 운영된다.

```text
k6/
  orders-test.js
  products-test.js
  points-test.js
  points-cursor-test.js
  points-cursor-sampling-test.js
  points-offset-sampling-test.js
  review-summary-test.js
  run.sh
  presets/
```

이 구조는 phase별 실험 의도를 파일 이름으로 바로 드러낸다는 장점이 있다. 예를 들어 `review-summary-test.js`는 Phase 6 aggregation evidence와 연결되고, `points-*` 계열은 Phase 7 pagination evidence와 연결된다.

다만 각 k6 script 안에는 다음과 같은 runtime plumbing이 반복된다.

- preset JSON 로딩
- `baseUrl`, `timeout`, rate, duration, VU 설정 해석
- `phase`, `scenario`, `preset`, `pool` label 구성
- k6 `options` 생성
- status code와 latency check
- 일부 scenario의 body shape check

기존 `2026-06-01-k6-js-module-separation-design.md`는 `reservation-test.js` 하나를 기준으로 작성되어 있다. 그러나 현재 프로젝트에는 reservation 전용 k6 entrypoint가 없고, 여러 ecommerce scenario entrypoint가 존재한다. 따라서 해당 spec의 의도는 유지하되, 현재 프로젝트 구조에 맞게 공통 k6 lib만 작게 도입한다.

## 목표

- scenario별 k6 entrypoint 파일은 유지한다.
- 반복되는 preset/env/options/check 코드를 `k6/lib/`로 분리한다.
- 1차 리팩토링 범위는 다음 세 모듈로 제한한다.

```text
k6/lib/
  config.js
  scenarios.js
  checks.js
```

- `review-summary-test.js`를 tracer bullet로 먼저 리팩토링한다.
- tracer bullet 검증 후 같은 패턴을 다른 k6 scenario 파일로 확장한다.
- 기존 `k6/run.sh`, `npm run k6:evidence`, `make k6-evidence`, `make evidence-capture` 실행 계약을 유지한다.
- 기존 Prometheus/Grafana label 계약인 `phase`, `scenario`, `preset`, `pool`을 유지한다.
- 기존 preset JSON 포맷을 유지한다.

## 비목표

- 이번 단계에서 모든 k6 script를 하나의 통합 runner로 합치지 않는다.
- 이번 단계에서 `k6/presets/*.json`을 YAML로 바꾸지 않는다.
- 이번 단계에서 `k6/run.sh`를 Node 기반 runner로 교체하지 않는다.
- 이번 단계에서 custom k6 Counter/Gauge metric을 새로 도입하지 않는다.
- 이번 단계에서 SQL consistency evidence 흐름을 k6 안으로 옮기지 않는다.
- 이번 단계에서 random/sampling helper를 별도 모듈로 분리하지 않는다.
- 이번 단계에서 부하 조건, threshold 수치, duration, VU 수를 실험 의도 없이 변경하지 않는다.

## 결정

현재 프로젝트에는 scenario별 k6 entrypoint를 유지하는 것이 맞다.

`orders-test.js`, `products-test.js`, `points-test.js`, `review-summary-test.js` 같은 파일은 "어떤 API를 어떤 방식으로 호출하는가"를 드러내는 실험 문서 역할도 한다. 이를 하나의 generic runner로 합치면 phase별 의도가 흐려지고, scenario별 query parameter/body 검증 차이가 runner 내부 조건문으로 숨을 수 있다.

따라서 이번 리팩토링은 entrypoint 통합이 아니라 공통 runtime plumbing 분리로 제한한다.

## 파일 구조

1차 리팩토링 후 목표 구조는 다음과 같다.

```text
k6/
  lib/
    config.js
    scenarios.js
    checks.js

  orders-test.js
  products-test.js
  points-test.js
  points-cursor-test.js
  points-cursor-sampling-test.js
  points-offset-sampling-test.js
  review-summary-test.js
  run.sh
  presets/
```

## `config.js`

`k6/lib/config.js`는 preset과 k6 환경변수를 읽어 정규화된 실행 설정을 만든다.

책임은 다음과 같다.

- `open(__ENV.PRESET || defaultPresetPath)`로 preset JSON을 읽는다.
- `baseUrl` 기본값을 제공한다.
- `timeout` 기본값을 제공한다.
- `phase`, `scenario`, `preset`, `pool` label 값을 만든다.
- request tag에 사용할 common tag 객체를 만든다.
- preset의 `thresholds` override를 지원한다.
- scenario별 기본값을 받을 수 있게 한다.

예상 API는 다음과 같다.

```js
export function loadConfig(defaults)
export function createRequestTags(config, name)
```

`defaults`는 scenario 파일에서 넘긴다.

```js
const config = loadConfig({
  defaultPresetPath: 'presets/review-summary-baseline.json',
  defaultPhase: 'phase-06',
  defaultScenario: 'review-summary',
  defaultPresetName: 'review-summary-baseline',
  defaultPool: 'pool10',
  defaultTimeout: '10s',
  defaultThresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<10000'],
  },
});
```

`PRESET`과 `PRESET_NAME`의 의미는 유지한다.

- `__ENV.PRESET`: k6가 읽을 preset 파일 경로
- `__ENV.PRESET_NAME`: Prometheus/Grafana label에 기록할 preset 이름

이 둘을 섞지 않는다.

## `scenarios.js`

`k6/lib/scenarios.js`는 k6 `options` 생성을 담당한다.

책임은 다음과 같다.

- `constant-arrival-rate` scenario option을 생성한다.
- `rate`, `duration`, `preAllocatedVUs`, `maxVUs` 값을 preset에서 읽는다.
- common tags와 system tags를 포함한다.
- thresholds를 포함한다.

예상 API는 다음과 같다.

```js
export function buildConstantArrivalRateOptions(config, defaults)
```

기본 system tags는 현재 계약을 유지한다.

```js
['status', 'method', 'name', 'expected_response']
```

이번 단계에서는 현재 k6 script들이 사용하는 `constant-arrival-rate`만 지원한다. 다른 executor가 필요해질 때 이 모듈에 추가한다.

## `checks.js`

`k6/lib/checks.js`는 반복되는 k6 `check()` 규칙을 작게 감싼다.

책임은 다음과 같다.

- HTTP 200 check 생성
- latency threshold check 생성
- JSON body array check 생성
- scenario 파일에서 필요한 check 조합을 명확하게 호출할 수 있게 한다.

예상 API는 다음과 같다.

```js
export function checkStatusOk(response)
export function checkResponseTimeBelow(response, milliseconds)
export function checkJsonArray(response)
```

또는 k6 `check()`를 한 번에 호출하는 helper를 둘 수도 있다.

```js
export function checkHttpOk(response, maxDurationMs)
export function checkHttpOkJsonArray(response, maxDurationMs)
```

정확한 함수 형태는 tracer bullet 구현 중 가장 읽기 쉬운 쪽으로 정한다. 중요한 기준은 scenario 파일에 어떤 검증이 필요한지 숨기지 않는 것이다.

## Tracer Bullet

첫 적용 대상은 `k6/review-summary-test.js`로 한다.

이 파일은 다음 이유로 tracer bullet에 적합하다.

- Phase 6 evidence 흐름과 직접 연결되어 있다.
- 기본 preset이 `review-summary-baseline.json`으로 scenario-specific하다.
- `status 200`, latency, body array 검증을 모두 포함한다.
- URL 구성이 단순해서 공통 lib 경계를 검증하기 쉽다.

리팩토링 후 `review-summary-test.js`는 다음 책임만 가진다.

- `loadConfig()`에 scenario 기본값을 전달한다.
- `/api/products/review-summary` request tag와 URL을 정의한다.
- HTTP GET을 실행한다.
- array body를 기대한다는 scenario-specific check를 호출한다.

예상 형태는 다음과 같다.

```js
import http from 'k6/http';
import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOkJsonArray } from './lib/checks.js';

const config = loadConfig({
  defaultPresetPath: 'presets/review-summary-baseline.json',
  defaultPhase: 'phase-06',
  defaultScenario: 'review-summary',
  defaultPresetName: 'review-summary-baseline',
  defaultPool: 'pool10',
  defaultTimeout: '10s',
  defaultThresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<10000'],
  },
});

const requestTags = createRequestTags(config, 'GET /api/products/review-summary');

export const options = buildConstantArrivalRateOptions(config, {
  defaultRate: 20,
  defaultDuration: '5m',
  defaultPreAllocatedVUs: 50,
  defaultMaxVUs: 150,
});

export default function () {
  const response = http.get(`${config.baseUrl}/api/products/review-summary`, {
    timeout: config.timeout,
    tags: requestTags,
  });

  checkHttpOkJsonArray(response, 10000);
}
```

## 확장 순서

Tracer bullet 검증 후 다음 순서로 확장한다.

1. `review-summary-test.js`
2. `orders-test.js`
3. `products-test.js`
4. `points-test.js`
5. `points-cursor-test.js`
6. `points-offset-sampling-test.js`
7. `points-cursor-sampling-test.js`

각 파일은 기존 동작을 유지하면서 공통 lib 사용으로만 변경한다.

`randomBetween`, weighted page sampling 같은 helper는 일단 scenario 파일에 남긴다. 여러 파일을 옮긴 뒤 중복이 실제로 문제가 되면 별도 `sampling.js` 추가를 검토한다.

## 기존 호환성

다음 실행 흐름은 유지되어야 한다.

```bash
npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --condition naive-index
make k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index
make evidence-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=naive-index TABLE=review
```

다음 label 계약도 유지되어야 한다.

```text
phase
scenario
preset
pool
```

Grafana dashboard와 run-window evidence는 이 label 계약에 의존한다.

## k6 Runtime 제약

`k6/lib/*.js`는 Node.js 모듈이 아니다.

따라서 다음을 사용하지 않는다.

- `fs`
- `path`
- `process`
- npm package
- Node 전용 API

사용 가능한 것은 k6가 지원하는 ES module import, `open()`, `__ENV`, k6 built-in module이다.

## 검증 전략

### 1차 검증

Tracer bullet 적용 후 다음을 확인한다.

- `review-summary-test.js`가 k6 runtime에서 import 오류 없이 실행된다.
- 기존 preset file path가 유지된다.
- `phase`, `scenario`, `preset`, `pool` label이 유지된다.
- 기존 threshold 기본값이 유지된다.
- body array check가 유지된다.

가능한 실행 명령은 다음과 같다.

```bash
npm run k6:evidence -- \
  --phase phase-06 \
  --scenario review-summary \
  --preset review-summary-baseline \
  --condition refactor-smoke
```

실제 k6 실행은 서버, DB, Prometheus 상태에 의존한다. 환경이 준비되지 않은 경우에는 import/runtime 오류 확인을 우선하고, 실제 evidence capture는 별도 검증으로 남긴다.

### 확장 후 검증

나머지 k6 script까지 확장한 뒤 다음을 확인한다.

```bash
npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --condition refactor-smoke
npm run k6:evidence -- --phase phase-07 --scenario points --preset points-page0 --condition refactor-smoke
```

가능하면 다음 Makefile 흐름도 확인한다.

```bash
make k6-evidence PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=refactor-smoke
make evidence-capture PHASE=phase-06 SCENARIO=review-summary PRESET=review-summary-baseline CONDITION=refactor-smoke TABLE=review
```

## 완료 기준

- `k6/lib/config.js`, `k6/lib/scenarios.js`, `k6/lib/checks.js`가 존재한다.
- `review-summary-test.js`가 공통 lib를 사용하는 얇은 scenario entrypoint로 바뀐다.
- 기존 k6 실행 명령 계약이 유지된다.
- 기존 Prometheus/Grafana label 계약이 유지된다.
- tracer bullet 검증이 통과한다.
- 나머지 k6 script 확장은 같은 패턴으로 진행 가능한 상태가 된다.
