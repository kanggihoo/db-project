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
