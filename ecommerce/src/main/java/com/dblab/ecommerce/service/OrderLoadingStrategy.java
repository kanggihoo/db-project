package com.dblab.ecommerce.service;

import java.util.Arrays;

public enum OrderLoadingStrategy {
    LAZY("lazy"),
    FETCH_JOIN("fetch-join"),
    BATCH_SIZE("batch-size"),
    ENTITY_GRAPH("entity-graph");

    private final String value;

    OrderLoadingStrategy(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OrderLoadingStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return LAZY;
        }
        return Arrays.stream(values())
                .filter(strategy -> strategy.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported order loading strategy: " + value));
    }
}
