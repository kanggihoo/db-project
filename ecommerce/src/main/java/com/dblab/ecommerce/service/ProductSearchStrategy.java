package com.dblab.ecommerce.service;

import java.util.Arrays;

public enum ProductSearchStrategy {
    BASELINE("baseline"),
    QUERYDSL("querydsl");

    private final String value;

    ProductSearchStrategy(String value) {
        this.value = value;
    }

    public static ProductSearchStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return QUERYDSL;
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(strategy -> strategy.value.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported product search strategy: " + value));
    }
}
