package com.dblab.ecommerce.dto;

import java.math.BigDecimal;

public record ProductReviewSummaryResponse(
        Long productId,
        String productName,
        BigDecimal avgRating,
        Long reviewCount) {
}
