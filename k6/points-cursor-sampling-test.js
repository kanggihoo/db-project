import http from 'k6/http';
import { Trend } from 'k6/metrics';
import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOk } from './lib/checks.js';

const config = loadConfig({
    defaultPresetPath: 'presets/points-cursor-sampling.json',
    defaultPhase: 'phase-07',
    defaultScenario: 'points-cursor-sampling',
    defaultPresetName: 'cursor-sampling',
    defaultPool: 'pool10',
    defaultTimeout: '5s',
    defaultThresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],
    },
});

const USER_ID = Number(config.preset.userId || 707000);
const SIZE = Number(config.preset.size || 20);
const SAMPLES = config.preset.samples || [{ page: 0, lastCreatedAt: null, lastId: null }];

const trends = {};
for (const sample of SAMPLES) {
    trends[sample.page] = new Trend(`points_cursor_page_${sample.page}_duration`, true);
}

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
    const sample = SAMPLES[Math.floor(Math.random() * SAMPLES.length)];
    let url = `${config.baseUrl}/api/points/cursor?userId=${USER_ID}&size=${SIZE}`;

    if (sample.lastCreatedAt && sample.lastId) {
        url += `&lastCreatedAt=${encodeURIComponent(sample.lastCreatedAt)}&lastId=${sample.lastId}`;
    }

    const res = http.get(url, {
        timeout: config.timeout,
        tags: {
            ...requestTags,
            name: 'GET /api/points/cursor',
            page_bucket: String(sample.page),
        },
    });

    trends[sample.page].add(res.timings.duration);

    checkHttpOk(res, 5000);
}
