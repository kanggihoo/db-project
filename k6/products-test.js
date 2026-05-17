import http from 'k6/http';
import { check } from 'k6';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/baseline.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const CATEGORY_START = Number(preset.categoryStart || 1);
const CATEGORY_END = Number(preset.categoryEnd || CATEGORY_START);
const STATUSES = preset.statuses || ['ON_SALE', 'SOLD_OUT', 'DISCONTINUED'];
const TIMEOUT = preset.timeout || '5s';

const commonTags = {
    phase: __ENV.PHASE || 'phase-01',
    scenario: __ENV.SCENARIO || 'products',
    preset: __ENV.PRESET_NAME || 'baseline',
    pool: __ENV.POOL || 'pool10',
};

const requestTags = {
    ...commonTags,
    name: 'GET /api/products',
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

function randomBetween(start, end) {
    return Math.floor(Math.random() * (end - start + 1)) + start;
}

export default function () {
    const categoryId = randomBetween(CATEGORY_START, CATEGORY_END);
    const status = STATUSES[Math.floor(Math.random() * STATUSES.length)];
    const res = http.get(`${BASE_URL}/api/products?categoryId=${categoryId}&status=${status}`, {
        timeout: TIMEOUT,
        tags: requestTags,
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });
}
