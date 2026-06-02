import { check } from 'k6';

function durationLabel(maxDurationMs) {
    if (maxDurationMs % 1000 === 0) {
        return `${maxDurationMs / 1000}s`;
    }

    return `${maxDurationMs}ms`;
}

export function checkHttpOk(response, maxDurationMs) {
    return check(response, {
        'status 200': (r) => r.status === 200,
        [`response time < ${durationLabel(maxDurationMs)}`]: (r) => r.timings.duration < maxDurationMs,
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
        [`response time < ${durationLabel(maxDurationMs)}`]: (r) => r.timings.duration < maxDurationMs,
        'body is array': responseIsJsonArray,
    });
}
