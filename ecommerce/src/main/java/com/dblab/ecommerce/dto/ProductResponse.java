package com.dblab.ecommerce.dto;

import com.dblab.ecommerce.entity.Product;

public record ProductResponse(
        Long productId,
        Long categoryId,
        String name,
        Integer basePrice,
        Product.Status status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getBasePrice(),
                product.getStatus()
        );
    }
}
