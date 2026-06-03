package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EntityGraphOrderLoadingStrategy implements OrderLoadingStrategy {

    private final OrderRepository orderRepository;

    @Override
    public String name() {
        return "entity-graph";
    }

    @Override
    public List<OrderResponse> loadByUserId(Long userId) {
        return orderRepository.findGraphByUserId(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }
}
