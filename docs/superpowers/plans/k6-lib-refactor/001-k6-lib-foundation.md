# 001 k6 Lib Foundation

## Goal

기존 k6 script의 공통 runtime plumbing을 담을 `k6/lib/config.js`, `k6/lib/scenarios.js`, `k6/lib/checks.js`를 생성한다.

## Files

- Create: `k6/lib/config.js`
- Create: `k6/lib/scenarios.js`
- Create: `k6/lib/checks.js`

## Steps

- [ ] **Step 1: Create `k6/lib/config.js`**

`loadConfig(defaults)`와 `createRequestTags(config, name)`를 구현한다.

`loadConfig(defaults)`는 다음 값을 반환한다.

```javascript
{
    preset,
    baseUrl,
    timeout,
    tags,
    thresholds
}
```

필수 동작:

- `JSON.parse(open(__ENV.PRESET || defaults.defaultPresetPath))`로 preset을 읽는다.
- `baseUrl`은 `preset.baseUrl || defaults.defaultBaseUrl || 'http://host.docker.internal:8080'` 순서로 결정한다.
- `timeout`은 `preset.timeout || defaults.defaultTimeout || '5s'` 순서로 결정한다.
- `tags.phase`는 `__ENV.PHASE || defaults.defaultPhase`로 결정한다.
- `tags.scenario`는 `__ENV.SCENARIO || defaults.defaultScenario`로 결정한다.
- `tags.preset`은 `__ENV.PRESET_NAME || defaults.defaultPresetName`으로 결정한다.
- `tags.pool`은 `__ENV.POOL || defaults.defaultPool || 'pool10'`으로 결정한다.
- `thresholds`는 `preset.thresholds === undefined ? defaults.defaultThresholds : preset.thresholds`로 결정한다.

- [ ] **Step 2: Create `createRequestTags`**

`createRequestTags(config, name)`는 common tags에 request `name`을 더한 객체를 반환한다.

```javascript
export function createRequestTags(config, name) {
    return {
        ...config.tags,
        name,
    };
}
```

- [ ] **Step 3: Create `k6/lib/scenarios.js`**

`buildConstantArrivalRateOptions(config, defaults)`를 구현한다.

필수 동작:

- `tags`는 `config.tags`를 사용한다.
- `systemTags`는 `['status', 'method', 'name', 'expected_response']`를 유지한다.
- executor는 `constant-arrival-rate`를 사용한다.
- `rate`, `duration`, `preAllocatedVUs`, `maxVUs`는 preset 값이 있으면 우선하고 없으면 defaults를 사용한다.
- `thresholds`는 `config.thresholds`를 사용한다.

- [ ] **Step 4: Create `k6/lib/checks.js`**

`checkHttpOk(response, maxDurationMs)`와 `checkHttpOkJsonArray(response, maxDurationMs)`를 구현한다.

필수 동작:

- `checkHttpOk`는 `status 200`과 `response time < Ns`를 검증한다.
- `checkHttpOkJsonArray`는 `checkHttpOk`의 의미에 더해 `body is array`를 검증한다.
- JSON parse 실패는 `false`를 반환한다.

함수명과 check label은 기존 k6 summary에서 읽기 쉬운 형태로 유지한다.

- [ ] **Step 5: Verify no Node-only API**

Run:

```bash
rtk rg -n "node:|require\\(|process\\.|fs\\.|path\\." k6/lib
```

Expected: command finds no Node-only API usage.

- [ ] **Step 6: Commit**

```bash
git add k6/lib/config.js k6/lib/scenarios.js k6/lib/checks.js
git commit -m "test(k6): add shared k6 lib helpers"
```
