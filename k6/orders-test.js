import http from 'k6/http';
import { check } from 'k6';

const preset = JSON.parse(open(__ENV.PRESET || 'presets/baseline.json'));

const BASE_URL = preset.baseUrl || 'http://host.docker.internal:8080';
const USER_START = Number(preset.userStart || 1);
const USER_END = Number(preset.userEnd || USER_START);
const TIMEOUT = preset.timeout || '5s';

export const options = {
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
    const userId = randomBetween(USER_START, USER_END);
    const res = http.get(`${BASE_URL}/api/orders?userId=${userId}`, {
        timeout: TIMEOUT,
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });
}
