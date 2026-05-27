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
