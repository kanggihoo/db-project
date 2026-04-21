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
const CATEGORY_COUNT = 20;
const STATUSES = ['ON_SALE', 'SOLD_OUT', 'DISCONTINUED'];

export default function () {
    // categoryId + status 조합을 랜덤으로 주입 — 다양한 풀스캔 조건 유발
    const categoryId = Math.floor(Math.random() * CATEGORY_COUNT) + 1;
    const status = STATUSES[Math.floor(Math.random() * STATUSES.length)];

    const res = http.get(`${BASE_URL}/api/products?categoryId=${categoryId}&status=${status}`, {
        timeout: '5s',
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });

    sleep(0.1);
}
