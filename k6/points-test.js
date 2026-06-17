import http from 'k6/http';

import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOk } from './lib/checks.js';

const config = loadConfig({
    defaultPresetPath: 'presets/baseline.json',
    defaultPhase: 'phase-01',
    defaultScenario: 'points',
    defaultPresetName: 'baseline',
    defaultPool: 'pool10',
    defaultTimeout: '5s',
    defaultThresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],
    },
});
const USER_START = Number(config.preset.userStart || 1);
const USER_END = Number(config.preset.userEnd || USER_START);
const SIZE = Number(config.preset.size || 20);

const requestTags = createRequestTags(config, 'GET /api/points');

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

function randomWeightedPage() {
    const rand = Math.random();
    if (rand < 0.5) {
        return randomBetween(1, 10);
    }
    if (rand < 0.8) {
        return randomBetween(50, 100);
    }
    return randomBetween(500, 1000);
}

export default function () {
    const userId = randomBetween(USER_START, USER_END);
    const page = config.preset.page === undefined ? randomWeightedPage() : Number(config.preset.page);
    const res = http.get(`${config.baseUrl}/api/points?userId=${userId}&page=${page}&size=${SIZE}`, {
        timeout: config.timeout,
        tags: requestTags,
    });

    checkHttpOk(res, 5000);
}
