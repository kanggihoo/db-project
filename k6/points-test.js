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

// Offset 병목 극대화를 위한 가중 분포 페이지 선택
// 50% → 초반(1~10), 30% → 중반(50~100), 20% → 후반(500~1000)
function randomPage() {
    const rand = Math.random();
    if (rand < 0.5) {
        return Math.floor(Math.random() * 10) + 1;
    } else if (rand < 0.8) {
        return Math.floor(Math.random() * 51) + 50;
    } else {
        return Math.floor(Math.random() * 501) + 500;
    }
}

export default function () {
    const userId = Math.floor(Math.random() * USER_COUNT) + 1;
    const page = randomPage();

    const res = http.get(`${BASE_URL}/api/points?userId=${userId}&page=${page}&size=20`, {
        timeout: '5s',
    });

    check(res, {
        'status 200': (r) => r.status === 200,
        'response time < 5s': (r) => r.timings.duration < 5000,
    });

    sleep(0.1);
}
