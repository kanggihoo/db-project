package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.entity.OrderItem;
import com.dblab.ecommerce.entity.Orders;
import com.dblab.ecommerce.repository.OrderItemRepository;
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
    private final OrderItemRepository orderItemRepository;

    // N+1 의도적 유발: 주문마다 OrderItem을 별도 쿼리로 조회
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Orders> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return OrderResponse.of(order, items);
        }).toList();
    }
}
