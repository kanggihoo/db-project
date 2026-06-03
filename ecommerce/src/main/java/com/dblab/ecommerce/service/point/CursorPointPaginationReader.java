package com.dblab.ecommerce.service.point;

import com.dblab.ecommerce.dto.PointHistoryCursor;
import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.entity.PointHistory;
import com.dblab.ecommerce.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CursorPointPaginationReader {

    private final PointHistoryRepository pointHistoryRepository;

    public PointHistoryCursorResponse read(
            Long userId,
            int size,
            LocalDateTime lastCreatedAt,
            Long lastId) {
        int limit = size + 1;
        List<PointHistory> rows = lastCreatedAt == null || lastId == null
                ? pointHistoryRepository.findFirstCursorPage(userId, PageRequest.of(0, limit))
                : pointHistoryRepository.findNextCursorPage(userId, lastCreatedAt, lastId, PageRequest.of(0, limit));

        boolean hasNext = rows.size() > size;
        List<PointHistory> pageRows = hasNext ? rows.subList(0, size) : rows;
        List<PointHistoryResponse> items = pageRows.stream()
                .map(PointHistoryResponse::from)
                .toList();
        PointHistoryCursor nextCursor = hasNext ? cursorFrom(pageRows.get(pageRows.size() - 1)) : null;
        return new PointHistoryCursorResponse(items, nextCursor, hasNext);
    }

    private PointHistoryCursor cursorFrom(PointHistory pointHistory) {
        return new PointHistoryCursor(pointHistory.getCreatedAt(), pointHistory.getId());
    }
}
