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
