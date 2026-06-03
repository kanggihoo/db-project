# 002 Review Summary Tracer Bullet

## Goal

`k6/review-summary-test.js` 하나를 먼저 새 `k6/lib/` 구조로 리팩토링해서 모듈 경계와 k6 runtime 호환성을 검증한다.

## Files

- Modify: `k6/review-summary-test.js`

## Steps

- [ ] **Step 1: Import shared lib modules**

기존 `check` import를 제거하고 다음 import를 추가한다.

```javascript
import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOkJsonArray } from './lib/checks.js';
```

`http` import는 유지한다.

- [ ] **Step 2: Replace preset/env plumbing with `loadConfig`**

`review-summary-test.js`의 preset/env/common tag 생성 코드를 `loadConfig()` 호출로 바꾼다.

필수 기본값:

```javascript
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

- [ ] **Step 3: Replace request tags**

기존 `requestTags` 객체를 `createRequestTags()` 호출로 바꾼다.

```javascript
const requestTags = createRequestTags(config, 'GET /api/products/review-summary');
```

- [ ] **Step 4: Replace options**

기존 `options` 객체를 `buildConstantArrivalRateOptions()` 호출로 바꾼다.

```javascript
export const options = buildConstantArrivalRateOptions(config, {
    defaultRate: 20,
    defaultDuration: '5m',
    defaultPreAllocatedVUs: 50,
    defaultMaxVUs: 150,
});
```

- [ ] **Step 5: Keep scenario-specific request visible**

default function은 `/api/products/review-summary` 호출을 직접 보여줘야 한다.

```javascript
export default function () {
    const response = http.get(`${config.baseUrl}/api/products/review-summary`, {
        timeout: config.timeout,
        tags: requestTags,
    });

    checkHttpOkJsonArray(response, 10000);
}
```

- [ ] **Step 6: Verify tracer bullet statically**

Run:

```bash
rtk rg -n "loadConfig|buildConstantArrivalRateOptions|checkHttpOkJsonArray|GET /api/products/review-summary|review-summary-baseline" k6/review-summary-test.js k6/lib
rtk proxy node scripts/verify-observability.mjs
```

Expected: commands exit 0.

- [ ] **Step 7: Verify runtime when environment is available**

Run when server, database, and k6 runtime are available:

```bash
rtk npm run k6:evidence -- --phase phase-06 --scenario review-summary --preset review-summary-baseline --condition refactor-smoke
```

Expected: k6 imports the shared lib modules and executes without module resolution errors.

- [ ] **Step 8: Commit**

```bash
git add k6/lib/config.js k6/lib/scenarios.js k6/lib/checks.js k6/review-summary-test.js
git commit -m "test(k6): refactor review summary scenario through shared lib"
```
