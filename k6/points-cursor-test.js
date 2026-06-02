import http from 'k6/http';
import { check } from 'k6';

import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOk } from './lib/checks.js';

const config = loadConfig({
    defaultPresetPath: 'presets/points-cursor.json',
    defaultPhase: 'phase-07',
    defaultScenario: 'points-cursor',
    defaultPresetName: 'cursor',
    defaultPool: 'pool10',
    defaultTimeout: '5s',
    defaultThresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],
    },
});
const USER_ID = Number(config.preset.userId || 1);
const SIZE = Number(config.preset.size || 20);

const requestTags = createRequestTags(config, 'GET /api/points/cursor');

export const options = {
    ...buildConstantArrivalRateOptions(config, {
        defaultRate: 50,
        defaultDuration: '5m',
        defaultPreAllocatedVUs: 100,
        defaultMaxVUs: 300,
    }),
};

export default function () {
    const first = http.get(`${config.baseUrl}/api/points/cursor?userId=${USER_ID}&size=${SIZE}`, {
        timeout: config.timeout,
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
    const next = http.get(`${config.baseUrl}/api/points/cursor?userId=${USER_ID}&size=${SIZE}&lastCreatedAt=${lastCreatedAt}&lastId=${lastId}`, {
        timeout: config.timeout,
        tags: requestTags,
    });

    checkHttpOk(next, 5000);
}
