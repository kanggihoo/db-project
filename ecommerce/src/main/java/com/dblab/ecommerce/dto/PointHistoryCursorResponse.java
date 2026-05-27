package com.dblab.ecommerce.dto;

import java.util.List;

public record PointHistoryCursorResponse(
        List<PointHistoryResponse> items,
        PointHistoryCursor nextCursor,
        boolean hasNext) {
}
