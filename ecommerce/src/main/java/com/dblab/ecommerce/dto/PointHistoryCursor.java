package com.dblab.ecommerce.dto;

import java.time.LocalDateTime;

public record PointHistoryCursor(
        LocalDateTime lastCreatedAt,
        Long lastId) {
}
