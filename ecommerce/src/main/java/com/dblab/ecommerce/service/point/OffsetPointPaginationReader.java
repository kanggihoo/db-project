package com.dblab.ecommerce.service.point;

import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OffsetPointPaginationReader {

    private final PointHistoryRepository pointHistoryRepository;

    public Page<PointHistoryResponse> read(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));
        return pointHistoryRepository.findByUserId(userId, pageRequest)
                .map(PointHistoryResponse::from);
    }
}
