# 004 k6 Pagination Scenarios

## Goal

Offset page별 k6 preset과 Cursor API k6 시나리오를 추가한다.

## Files

- Modify: `k6/points-test.js`
- Create: `k6/points-cursor-test.js`
- Create: `k6/presets/points-mid.json`
- Create: `k6/presets/points-deep.json`
- Modify: `k6/presets/points-page0.json`
- Create: `k6/presets/points-cursor.json`
- Modify: `docs/guides/k6-load-testing.md`
- Modify: `scripts/verify-observability.mjs`

## Steps

- [ ] **Step 1: Keep existing Offset k6 but allow selected user**

Modify `k6/points-test.js` so it continues to support `userStart`/`userEnd` and fixed `page`. No new high-cardinality labels are added. If the current file already has this behavior, leave it unchanged.

Expected request shape:

```javascript
http.get(`${BASE_URL}/api/points?userId=${userId}&page=${page}&size=${SIZE}`, {
    timeout: TIMEOUT,
    tags: requestTags,
});
```

- [ ] **Step 2: Add cursor k6 scenario**

Create `k6/points-cursor-test.js`:

```javascript
import http from 'k6/http';
import { check } from 'k6';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/points-cursor.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const USER_ID = Number(preset.userId || 1);
const SIZE = Number(preset.size || 20);
const TIMEOUT = preset.timeout || '5s';

const commonTags = {
    phase: __ENV.PHASE || 'phase-07',
    scenario: __ENV.SCENARIO || 'points-cursor',
    preset: __ENV.PRESET_NAME || 'cursor',
    pool: __ENV.POOL || 'pool10',
};

const requestTags = {
    ...commonTags,
    name: 'GET /api/points/cursor',
};

export const options = {
    tags: commonTags,
    systemTags: ['status', 'method', 'name', 'expected_response'],
    scenarios: {
        steady: {
            executor: 'constant-arrival-rate',
            rate: Number(preset.rate || 50),
            timeUnit: '1s',
            duration: preset.duration || '5m',
            preAllocatedVUs: Number(preset.preAllocatedVUs || 100),
            maxVUs: Number(preset.maxVUs || 300),
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],
    },
};

export default function () {
    const first = http.get(`${BASE_URL}/api/points/cursor?userId=${USER_ID}&size=${SIZE}`, {
        timeout: TIMEOUT,
        tags: requestTags,
    });

    const firstOk = check(first, {
        'first status 200': (r) => r.status === 200,
        'first response has body': (r) => r.body && r.body.length > 0,
    });

    if (!firstOk) {
        return;
    }

    const body = JSON.parse(first.body);
    if (!body.hasNext || !body.nextCursor) {
        return;
    }

    const lastCreatedAt = encodeURIComponent(body.nextCursor.lastCreatedAt);
    const lastId = body.nextCursor.lastId;
    const next = http.get(`${BASE_URL}/api/points/cursor?userId=${USER_ID}&size=${SIZE}&lastCreatedAt=${lastCreatedAt}&lastId=${lastId}`, {
        timeout: TIMEOUT,
        tags: requestTags,
    });

    check(next, {
        'next status 200': (r) => r.status === 200,
        'next response time < 5s': (r) => r.timings.duration < 5000,
    });
}
```

- [ ] **Step 3: Normalize Offset page0 preset**

Modify `k6/presets/points-page0.json`:

```json
{
  "baseUrl": "http://host.docker.internal:8080",
  "userStart": 1,
  "userEnd": 1,
  "page": 0,
  "size": 20,
  "rate": 50,
  "duration": "5m",
  "preAllocatedVUs": 100,
  "maxVUs": 300,
  "timeout": "5s"
}
```

- [ ] **Step 4: Add Offset mid preset**

Create `k6/presets/points-mid.json`:

```json
{
  "baseUrl": "http://host.docker.internal:8080",
  "userStart": 1,
  "userEnd": 1,
  "page": 45,
  "size": 20,
  "rate": 50,
  "duration": "5m",
  "preAllocatedVUs": 100,
  "maxVUs": 300,
  "timeout": "5s"
}
```

- [ ] **Step 5: Add Offset deep preset**

Create `k6/presets/points-deep.json`:

```json
{
  "baseUrl": "http://host.docker.internal:8080",
  "userStart": 1,
  "userEnd": 1,
  "page": 72,
  "size": 20,
  "rate": 50,
  "duration": "5m",
  "preAllocatedVUs": 100,
  "maxVUs": 300,
  "timeout": "5s"
}
```

After data profiling, replace `userStart`, `userEnd`, `page` values with selected `user_id`, `midPage`, and `deepPage`.

- [ ] **Step 6: Add Cursor preset**

Create `k6/presets/points-cursor.json`:

```json
{
  "baseUrl": "http://host.docker.internal:8080",
  "userId": 1,
  "size": 20,
  "rate": 50,
  "duration": "5m",
  "preAllocatedVUs": 100,
  "maxVUs": 300,
  "timeout": "5s"
}
```

After data profiling, replace `userId` with selected `user_id`.

- [ ] **Step 7: Update k6 guide**

Append to `docs/guides/k6-load-testing.md`:

````md
## Phase 7 Pagination

Phase 7 uses `phase=phase-07` and keeps page/user values out of metric labels. Page depth is represented by `preset`.

```bash
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=page0 PRESET=presets/points-page0.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=mid PRESET=presets/points-mid.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=deep PRESET=presets/points-deep.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-cursor PRESET_NAME=cursor PRESET=presets/points-cursor.json k6 run k6/points-cursor-test.js
```
````

- [ ] **Step 8: Update observability verifier**

Modify `scripts/verify-observability.mjs` so it verifies the new cursor scenario:

```javascript
verifyScenario('k6/points-cursor-test.js', 'points-cursor', 'GET /api/points/cursor');
```

- [ ] **Step 9: Verify k6 files**

Run:

```bash
rtk rg -n "points-cursor|points-offset|PRESET_NAME=deep|GET /api/points/cursor|phase-07" k6 docs/guides scripts/verify-observability.mjs
rtk node scripts/verify-observability.mjs
```

Expected: commands exit 0.

- [ ] **Step 10: Commit**

```bash
git add k6/points-test.js k6/points-cursor-test.js k6/presets/points-page0.json k6/presets/points-mid.json k6/presets/points-deep.json k6/presets/points-cursor.json docs/guides/k6-load-testing.md scripts/verify-observability.mjs
git commit -m "test(phase7): add pagination k6 scenarios"
```
