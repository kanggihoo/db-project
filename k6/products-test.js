import http from 'k6/http';
import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOk } from './lib/checks.js';

const config = loadConfig({
    defaultPresetPath: 'presets/baseline.json',
    defaultPhase: 'phase-01',
    defaultScenario: 'products',
    defaultPresetName: 'baseline',
    defaultPool: 'pool10',
    defaultTimeout: '5s',
    defaultThresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],
    },
});
const CATEGORY_START = Number(config.preset.categoryStart || 1);
const CATEGORY_END = Number(config.preset.categoryEnd || CATEGORY_START);
const STATUSES = config.preset.statuses || ['ON_SALE', 'SOLD_OUT', 'DISCONTINUED'];

const requestTags = createRequestTags(config, 'GET /api/products');

export const options = {
    ...buildConstantArrivalRateOptions(config, {
        defaultRate: 50,
        defaultDuration: '5m',
        defaultPreAllocatedVUs: 100,
        defaultMaxVUs: 300,
    }),
};

function randomBetween(start, end) {
    return Math.floor(Math.random() * (end - start + 1)) + start;
}

export default function () {
    const categoryId = randomBetween(CATEGORY_START, CATEGORY_END);
    const status = STATUSES[Math.floor(Math.random() * STATUSES.length)];
    const res = http.get(`${config.baseUrl}/api/products?categoryId=${categoryId}&status=${status}`, {
        timeout: config.timeout,
        tags: requestTags,
    });

    checkHttpOk(res, 5000);
}
