# 014 k6 Sampling Scenarios

## Goal

Offset/Page와 Cursor next-slice를 같은 logical page sample 기준으로 측정하는 k6 sampling 시나리오를 추가한다.

## Files

- Create: `k6/points-offset-sampling-test.js`
- Create: `k6/points-cursor-sampling-test.js`
- Create: `k6/presets/points-offset-sampling.json`
- Create: `k6/presets/points-cursor-sampling.json`

## Steps

- [ ] **Step 1: Create Offset sampling preset**

Create `k6/presets/points-offset-sampling.json`.

```json
{
  "baseUrl": "http://host.docker.internal:8080",
  "userId": 707000,
  "size": 20,
  "rate": 50,
  "duration": "5m",
  "preAllocatedVUs": 100,
  "maxVUs": 300,
  "timeout": "5s",
  "pages": [0, 10, 50, 100, 500, 1000, 2000, 3000, 4000, 4999]
}
```

- [ ] **Step 2: Create Offset sampling k6 script**

Create `k6/points-offset-sampling-test.js`.

```javascript
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/points-offset-sampling.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const USER_ID = Number(preset.userId || 707000);
const SIZE = Number(preset.size || 20);
const TIMEOUT = preset.timeout || '5s';
const PAGES = preset.pages || [0, 10, 50, 100, 500, 1000, 2000, 3000, 4000, 4999];

const trends = {};
for (const page of PAGES) {
    trends[page] = new Trend(`points_offset_page_${page}_duration`, true);
}

const commonTags = {
    phase: __ENV.PHASE || 'phase-07',
    scenario: __ENV.SCENARIO || 'points-offset-sampling',
    preset: __ENV.PRESET_NAME || 'offset-sampling',
    pool: __ENV.POOL || 'pool10',
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
    const page = PAGES[Math.floor(Math.random() * PAGES.length)];
    const res = http.get(`${BASE_URL}/api/points?userId=${USER_ID}&page=${page}&size=${SIZE}`, {
        timeout: TIMEOUT,
        tags: {
            ...commonTags,
            name: 'GET /api/points',
            page_bucket: String(page),
        },
    });

    trends[page].add(res.timings.duration);

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });
}
```

- [ ] **Step 3: Create Cursor sampling preset**

Create `k6/presets/points-cursor-sampling.json` from `retest-cursor-samples.txt`.

```json
{
  "baseUrl": "http://host.docker.internal:8080",
  "userId": 707000,
  "size": 20,
  "rate": 50,
  "duration": "5m",
  "preAllocatedVUs": 100,
  "maxVUs": 300,
  "timeout": "5s",
  "samples": [
    { "page": 0, "lastCreatedAt": null, "lastId": null },
    { "page": 10, "lastCreatedAt": "2026-05-26T23:56:40", "lastId": 707000200 },
    { "page": 50, "lastCreatedAt": "2026-05-26T23:43:20", "lastId": 707001000 },
    { "page": 100, "lastCreatedAt": "2026-05-26T23:26:40", "lastId": 707002000 },
    { "page": 500, "lastCreatedAt": "2026-05-26T21:13:20", "lastId": 707010000 },
    { "page": 1000, "lastCreatedAt": "2026-05-26T18:26:40", "lastId": 707020000 },
    { "page": 2000, "lastCreatedAt": "2026-05-26T12:53:20", "lastId": 707040000 },
    { "page": 3000, "lastCreatedAt": "2026-05-26T07:20:00", "lastId": 707060000 },
    { "page": 4000, "lastCreatedAt": "2026-05-26T01:46:40", "lastId": 707080000 },
    { "page": 4999, "lastCreatedAt": "2026-05-25T20:13:40", "lastId": 707099980 }
  ]
}
```

These values match the deterministic rows inserted by `scripts/phase-07/05-hot-user-amplify.sql`. If `32-retest-cursor-samples.sql` produces different values, stop and inspect the seed state before running k6.

- [ ] **Step 4: Create Cursor sampling k6 script**

Create `k6/points-cursor-sampling-test.js`.

```javascript
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/points-cursor-sampling.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const USER_ID = Number(preset.userId || 707000);
const SIZE = Number(preset.size || 20);
const TIMEOUT = preset.timeout || '5s';
const SAMPLES = preset.samples || [{ page: 0, lastCreatedAt: null, lastId: null }];

const trends = {};
for (const sample of SAMPLES) {
    trends[sample.page] = new Trend(`points_cursor_page_${sample.page}_duration`, true);
}

const commonTags = {
    phase: __ENV.PHASE || 'phase-07',
    scenario: __ENV.SCENARIO || 'points-cursor-sampling',
    preset: __ENV.PRESET_NAME || 'cursor-sampling',
    pool: __ENV.POOL || 'pool10',
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
    const sample = SAMPLES[Math.floor(Math.random() * SAMPLES.length)];
    let url = `${BASE_URL}/api/points/cursor?userId=${USER_ID}&size=${SIZE}`;

    if (sample.lastCreatedAt && sample.lastId) {
        url += `&lastCreatedAt=${encodeURIComponent(sample.lastCreatedAt)}&lastId=${sample.lastId}`;
    }

    const res = http.get(url, {
        timeout: TIMEOUT,
        tags: {
            ...commonTags,
            name: 'GET /api/points/cursor',
            page_bucket: String(sample.page),
        },
    });

    trends[sample.page].add(res.timings.duration);

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });
}
```

- [ ] **Step 5: Smoke-test k6 scripts**

Run:

```bash
rtk docker compose run --rm k6 run /scripts/points-offset-sampling-test.js --vus 1 --iterations 1
rtk docker compose run --rm k6 run /scripts/points-cursor-sampling-test.js --vus 1 --iterations 1
```

Expected: both scripts return status `200` checks.

## Done When

- [ ] Offset sampling script emits `points_offset_page_*_duration` Trend metrics.
- [ ] Cursor sampling script emits `points_cursor_page_*_duration` Trend metrics.
- [ ] Cursor sampling sends one request per iteration.
- [ ] Cursor preset contains all 10 sample pages with real cursor values.
