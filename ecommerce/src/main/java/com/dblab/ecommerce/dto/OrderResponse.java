package com.dblab.ecommerce.dto;

import com.dblab.ecommerce.entity.OrderItem;
import com.dblab.ecommerce.entity.Orders;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long userId,
        Orders.Status status,
        Integer finalPrice,
        LocalDateTime createdAt,
        List<OrderItemDto> items) {
    public record OrderItemDto(
            Long itemId,
            String productName,
            Integer quantity,
            Integer unitPrice) {
        public static OrderItemDto from(OrderItem item) {
            return new OrderItemDto(item.getId(), item.getProductName(), item.getQuantity(), item.getUnitPrice());
        }
    }

    public static OrderResponse of(Orders order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getFinalPrice(),
                order.getCreatedAt(),
                items.stream().map(OrderItemDto::from).toList());
    }
}
