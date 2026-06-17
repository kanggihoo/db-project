package com.dblab.ecommerce.service.order;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderLoadingStrategyRegistry {

    private static final String DEFAULT_STRATEGY = "lazy";

    private final Map<String, OrderLoadingStrategy> strategies;

    public OrderLoadingStrategyRegistry(List<OrderLoadingStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        OrderLoadingStrategy::name,
                        Function.identity(),
                        (left, right) -> {
                            throw new IllegalStateException("Duplicate order loading strategy: " + left.name());
                        }
                ));
    }

    public OrderLoadingStrategy get(String name) {
        String strategyName = name == null || name.isBlank() ? DEFAULT_STRATEGY : name;
        OrderLoadingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported order loading strategy: " + strategyName);
        }
        return strategy;
    }
}
