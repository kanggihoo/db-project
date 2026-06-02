package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.service.point.CursorPointPaginationReader;
import com.dblab.ecommerce.service.point.OffsetPointPaginationReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final OffsetPointPaginationReader offsetPointPaginationReader;
    private final CursorPointPaginationReader cursorPointPaginationReader;

    // Offset 페이징: 뒤 페이지로 갈수록 병목 발생
    public Page<PointHistoryResponse> getPointHistory(Long userId, int page, int size) {
        return offsetPointPaginationReader.read(userId, page, size);
    }

    public PointHistoryCursorResponse getPointHistoryByCursor(
            Long userId,
            int size,
            LocalDateTime lastCreatedAt,
            Long lastId) {
        return cursorPointPaginationReader.read(userId, size, lastCreatedAt, lastId);
    }
}
