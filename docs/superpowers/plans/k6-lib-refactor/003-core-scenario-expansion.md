# 003 Core Scenario Expansion

## Goal

`review-summary-test.js`에서 검증된 패턴을 `orders-test.js`, `products-test.js`, `points-test.js`로 확장한다.

## Files

- Modify: `k6/orders-test.js`
- Modify: `k6/products-test.js`
- Modify: `k6/points-test.js`

## Steps

- [ ] **Step 1: Refactor `orders-test.js`**

공통 lib를 import한다.

```javascript
import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOk } from './lib/checks.js';
```

기존 scenario-specific 값은 파일에 유지한다.

- `USER_START`
- `USER_END`
- `STRATEGY`
- `randomBetween()`
- `/api/orders?userId=...&strategy=...`

기본값:

- `defaultPresetPath`: `presets/baseline.json`
- `defaultPhase`: `phase-01`
- `defaultScenario`: `orders`
- `defaultPresetName`: `baseline`
- `defaultTimeout`: `5s`
- `defaultRate`: `50`
- `defaultDuration`: `5m`
- `defaultPreAllocatedVUs`: `100`
- `defaultMaxVUs`: `300`
- `defaultThresholds.http_req_duration`: `p(95)<5000`

- [ ] **Step 2: Refactor `products-test.js`**

공통 lib를 import하고 preset/options/check plumbing을 제거한다.

기존 scenario-specific 값은 파일에 유지한다.

- `CATEGORY_START`
- `CATEGORY_END`
- `STATUSES`
- `randomBetween()`
- `/api/products?categoryId=...&status=...`

기본값은 orders와 동일하되 `defaultScenario`는 `products`로 둔다.

- [ ] **Step 3: Refactor `points-test.js`**

공통 lib를 import하고 preset/options/check plumbing을 제거한다.

기존 scenario-specific 값은 파일에 유지한다.

- `USER_START`
- `USER_END`
- `SIZE`
- `randomBetween()`
- `randomWeightedPage()`
- `/api/points?userId=...&page=...&size=...`

기본값은 orders와 동일하되 `defaultScenario`는 `points`로 둔다.

- [ ] **Step 4: Preserve threshold override behavior**

`orders-test.js`는 현재 `preset.thresholds` override를 지원한다. 리팩토링 후 `products-test.js`와 `points-test.js`도 동일하게 `preset.thresholds`를 사용할 수 있어야 한다.

단, 기존 preset 파일에 threshold가 없으면 기존 기본 threshold와 동일해야 한다.

- [ ] **Step 5: Verify core scenario files**

Run:

```bash
rtk rg -n "loadConfig|buildConstantArrivalRateOptions|checkHttpOk|GET /api/orders|GET /api/products|GET /api/points" k6/orders-test.js k6/products-test.js k6/points-test.js
rtk proxy node scripts/verify-observability.mjs
```

Expected: commands exit 0.

- [ ] **Step 6: Optional runtime smoke**

Run when environment is available:

```bash
rtk npm run k6:evidence -- --phase phase-07 --scenario points --preset points-page0 --condition refactor-smoke
```

Expected: k6 executes without module resolution errors and keeps labels intact.

- [ ] **Step 7: Commit**

```bash
git add k6/orders-test.js k6/products-test.js k6/points-test.js
git commit -m "test(k6): apply shared lib to core scenarios"
```
