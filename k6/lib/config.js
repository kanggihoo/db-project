export function loadConfig(defaults) {
    const preset = JSON.parse(open(__ENV.PRESET || defaults.defaultPresetPath));

    return {
        preset,
        baseUrl: preset.baseUrl || defaults.defaultBaseUrl || 'http://host.docker.internal:8080',
        timeout: preset.timeout || defaults.defaultTimeout || '5s',
        tags: {
            phase: __ENV.PHASE || defaults.defaultPhase,
            scenario: __ENV.SCENARIO || defaults.defaultScenario,
            preset: __ENV.PRESET_NAME || defaults.defaultPresetName,
            pool: __ENV.POOL || defaults.defaultPool || 'pool10',
        },
        thresholds: preset.thresholds === undefined ? defaults.defaultThresholds : preset.thresholds,
    };
}

export function createRequestTags(config, name) {
    return {
        ...config.tags,
        name,
    };
}
