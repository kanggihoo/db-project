package com.dblab.ecommerce.dto;

import com.dblab.ecommerce.entity.OrderItem;
import com.dblab.ecommerce.entity.Orders;
import com.dblab.ecommerce.entity.ProductImage;

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
            Integer unitPrice,
            String thumbnailUrl) {
        public static OrderItemDto from(OrderItem item) {
            return new OrderItemDto(
                    item.getId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    findThumbnailUrl(item));
        }

        private static String findThumbnailUrl(OrderItem item) {
            if (item.getProductSku() == null || item.getProductSku().getProduct() == null) {
                return null;
            }
            return item.getProductSku().getProduct().getImages().stream()
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
        }
    }

    public static OrderResponse from(Orders order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getFinalPrice(),
                order.getCreatedAt(),
                order.getOrderItems().stream().map(OrderItemDto::from).toList());
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
