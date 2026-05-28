package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.PointHistoryCursor;
import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.entity.PointHistory;
import com.dblab.ecommerce.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;

    // Offset 페이징: 뒤 페이지로 갈수록 병목 발생
    public Page<PointHistoryResponse> getPointHistory(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));
        return pointHistoryRepository.findByUserId(userId, pageRequest)
                .map(PointHistoryResponse::from);
    }

    public PointHistoryCursorResponse getPointHistoryByCursor(
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
