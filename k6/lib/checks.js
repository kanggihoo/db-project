import { check } from 'k6';

export function checkHttpOk(response, maxDurationMs) {
    return check(response, {
        'status 200': (r) => r.status === 200,
        [`response time < ${maxDurationMs}ms`]: (r) => r.timings.duration < maxDurationMs,
    });
}

export function checkHttpOkJsonArray(response, maxDurationMs) {
    const responseIsJsonArray = (res) => {
        try {
            return Array.isArray(JSON.parse(res.body));
        } catch (error) {
            return false;
        }
    };

    return check(response, {
        'status 200': (r) => r.status === 200,
        [`response time < ${maxDurationMs}ms`]: (r) => r.timings.duration < maxDurationMs,
        'body is array': responseIsJsonArray,
    });
}
