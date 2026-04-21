import http from 'k6/http';
import { check, sleep } from 'k6';

// 공통 스펙: VU 50, 5분, Ramp-up 30초
export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '4m30s', target: 50 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<5000'],
    },
};

const BASE_URL = 'http://host.docker.internal:8080';
const USER_COUNT = 1000;

export default function () {
    // userId를 1~1000 사이에서 랜덤으로 주입 — 캐싱 우회
    const userId = Math.floor(Math.random() * USER_COUNT) + 1;

    const res = http.get(`${BASE_URL}/api/orders?userId=${userId}`, {
        timeout: '5s',
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });

    sleep(0.1);
}
