package com.dblab.ecommerce.dto;

import com.dblab.ecommerce.entity.PointHistory;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long id,
        Long userId,
        PointHistory.Type type,
        Integer amount,
        Integer balanceAfter,
        String description,
        LocalDateTime createdAt) {
    public static PointHistoryResponse from(PointHistory ph) {
        return new PointHistoryResponse(
                ph.getId(),
                ph.getUserId(),
                ph.getType(),
                ph.getAmount(),
                ph.getBalanceAfter(),
                ph.getDescription(),
                ph.getCreatedAt());
    }
}
