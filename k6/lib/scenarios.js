export function buildConstantArrivalRateOptions(config, defaults) {
    return {
        tags: config.tags,
        systemTags: ['status', 'method', 'name', 'expected_response'],
        scenarios: {
            steady: {
                executor: 'constant-arrival-rate',
                rate: Number(config.preset.rate ?? defaults.defaultRate),
                timeUnit: '1s',
                duration: config.preset.duration || defaults.defaultDuration,
                preAllocatedVUs: Number(config.preset.preAllocatedVUs ?? defaults.defaultPreAllocatedVUs),
                maxVUs: Number(config.preset.maxVUs ?? defaults.defaultMaxVUs),
            },
        },
        thresholds: config.thresholds,
    };
}
