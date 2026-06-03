import http from 'k6/http';
import { loadConfig, createRequestTags } from './lib/config.js';
import { buildConstantArrivalRateOptions } from './lib/scenarios.js';
import { checkHttpOkJsonArray } from './lib/checks.js';

const config = loadConfig({
    defaultPresetPath: 'presets/review-summary-baseline.json',
    defaultPhase: 'phase-06',
    defaultScenario: 'review-summary',
    defaultPresetName: 'review-summary-baseline',
    defaultPool: 'pool10',
    defaultTimeout: '10s',
    defaultThresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<10000'],
    },
});

const requestTags = createRequestTags(config, 'GET /api/products/review-summary');

export const options = buildConstantArrivalRateOptions(config, {
    defaultRate: 20,
    defaultDuration: '5m',
    defaultPreAllocatedVUs: 50,
    defaultMaxVUs: 150,
});

export default function () {
    const response = http.get(`${config.baseUrl}/api/products/review-summary`, {
        timeout: config.timeout,
        tags: requestTags,
    });

    checkHttpOkJsonArray(response, 10000);
}
