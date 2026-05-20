package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.entity.Orders;
import com.dblab.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return getOrdersByUserId(userId, OrderLoadingStrategy.LAZY);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategy strategy) {
        return switch (strategy) {
            case LAZY -> getOrdersByUserIdLazy(userId);
            case FETCH_JOIN -> getOrdersByUserIdFetchJoin(userId);
            case BATCH_SIZE, ENTITY_GRAPH -> getOrdersByUserIdLazy(userId);
        };
    }

    private List<OrderResponse> getOrdersByUserIdLazy(Long userId) {
        List<Orders> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(OrderResponse::from).toList();
    }

    private List<OrderResponse> getOrdersByUserIdFetchJoin(Long userId) {
        return orderRepository.findByUserIdWithFetchJoin(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}
