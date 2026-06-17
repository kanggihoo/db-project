package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.service.order.OrderLoadingStrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderLoadingStrategyRegistry orderLoadingStrategyRegistry;

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return getOrdersByUserId(userId, "lazy");
    }

    public List<OrderResponse> getOrdersByUserId(Long userId, String strategyName) {
        return orderLoadingStrategyRegistry.get(strategyName).loadByUserId(userId);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategyName strategyName) {
        return getOrdersByUserId(userId, strategyName.value());
    }
}
