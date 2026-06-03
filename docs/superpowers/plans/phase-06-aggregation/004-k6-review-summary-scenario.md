# 004 k6 Review Summary Scenario

## Goal

Add a k6 scenario and preset for `GET /api/products/review-summary`.

## Files

- Create: `k6/review-summary-test.js`
- Create: `k6/presets/review-summary-baseline.json`
- Modify: `docs/guides/k6-load-testing.md`

## Steps

- [ ] **Step 1: Add review summary preset**

Create `k6/presets/review-summary-baseline.json`:

```json
{
  "baseUrl": "http://host.docker.internal:8080",
  "rate": 20,
  "duration": "5m",
  "preAllocatedVUs": 50,
  "maxVUs": 150,
  "timeout": "10s"
}
```

- [ ] **Step 2: Add k6 scenario**

Create `k6/review-summary-test.js`:

```javascript
import http from 'k6/http';
import { check } from 'k6';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/review-summary-baseline.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const TIMEOUT = preset.timeout || '10s';

const commonTags = {
    phase: __ENV.PHASE || 'phase-06',
    scenario: __ENV.SCENARIO || 'review-summary',
    preset: __ENV.PRESET_NAME || 'review-summary-baseline',
    pool: __ENV.POOL || 'pool10',
};

const requestTags = {
    ...commonTags,
    name: 'GET /api/products/review-summary',
};

export const options = {
    tags: commonTags,
    systemTags: ['status', 'method', 'name', 'expected_response'],
    scenarios: {
        steady: {
            executor: 'constant-arrival-rate',
            rate: Number(preset.rate || 20),
            timeUnit: '1s',
            duration: preset.duration || '5m',
            preAllocatedVUs: Number(preset.preAllocatedVUs || 50),
            maxVUs: Number(preset.maxVUs || 150),
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<10000'],
    },
};

export default function () {
    const res = http.get(`${BASE_URL}/api/products/review-summary`, {
        timeout: TIMEOUT,
        tags: requestTags,
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 10s': (r) => r.timings.duration < 10000,
        'body is array': (r) => {
            try {
                return Array.isArray(r.json());
            } catch (error) {
                return false;
            }
        },
    });
}
```

- [ ] **Step 3: Update k6 guide scenario table**

Modify `docs/guides/k6-load-testing.md` so the scenario table includes:

```markdown
| `review-summary` | `GET /api/products/review-summary` | Phase 6 aggregation API representative evidence |
```

Modify the preset table so it includes:

```markdown
| `review-summary-baseline` | 20 rps | 5m | Phase 6 review summary API comparison |
```

Add an example command:

```bash
PHASE=phase-06 POOL=pool10 ./k6/run.sh review-summary review-summary-baseline prometheus
```

- [ ] **Step 4: Validate k6 files are discoverable**

Run:

```bash
rtk powershell -NoProfile -Command "if (-not (Test-Path 'k6/review-summary-test.js')) { throw 'missing scenario' }; if (-not (Test-Path 'k6/presets/review-summary-baseline.json')) { throw 'missing preset' }"
```

Expected: command exits 0.

- [ ] **Step 5: Commit**

```bash
git add k6/review-summary-test.js k6/presets/review-summary-baseline.json docs/guides/k6-load-testing.md
git commit -m "test(phase6): add review summary k6 scenario"
```
