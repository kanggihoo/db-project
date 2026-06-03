import http from 'k6/http';
import { Trend } from 'k6/metrics';
import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOk } from './lib/checks.js';

const config = loadConfig({
    defaultPresetPath: 'presets/points-offset-sampling.json',
    defaultPhase: 'phase-07',
    defaultScenario: 'points-offset-sampling',
    defaultPresetName: 'offset-sampling',
    defaultPool: 'pool10',
    defaultTimeout: '5s',
    defaultThresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],
    },
});

const USER_ID = Number(config.preset.userId || 707000);
const SIZE = Number(config.preset.size || 20);
const PAGES = config.preset.pages || [0, 10, 50, 100, 500, 1000, 2000, 3000, 4000, 4999];

const trends = {};
for (const page of PAGES) {
    trends[page] = new Trend(`points_offset_page_${page}_duration`, true);
}

const requestTags = createRequestTags(config, 'GET /api/points');

export const options = {
    ...buildConstantArrivalRateOptions(config, {
        defaultRate: 50,
        defaultDuration: '5m',
        defaultPreAllocatedVUs: 100,
        defaultMaxVUs: 300,
    }),
};

export default function () {
    const page = PAGES[Math.floor(Math.random() * PAGES.length)];
    const res = http.get(`${config.baseUrl}/api/points?userId=${USER_ID}&page=${page}&size=${SIZE}`, {
        timeout: config.timeout,
        tags: {
            ...requestTags,
            name: 'GET /api/points',
            page_bucket: String(page),
        },
    });

    trends[page].add(res.timings.duration);

    checkHttpOk(res, 5000);
}
