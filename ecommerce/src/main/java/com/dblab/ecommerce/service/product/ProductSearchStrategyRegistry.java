package com.dblab.ecommerce.service.product;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProductSearchStrategyRegistry {

    private static final String DEFAULT_STRATEGY = "querydsl";

    private final Map<String, ProductSearchStrategy> strategies;

    public ProductSearchStrategyRegistry(List<ProductSearchStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        strategy -> normalize(strategy.name()),
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate product search strategy: " + normalize(left.name()));
                        }
                ));
    }

    public ProductSearchStrategy get(String name) {
        String strategyName = name == null || name.isBlank() ? DEFAULT_STRATEGY : normalize(name);
        ProductSearchStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported product search strategy: " + name);
        }
        return strategy;
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
