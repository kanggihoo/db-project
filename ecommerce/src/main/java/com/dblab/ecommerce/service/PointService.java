package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;

    // Offset 페이징: 뒤 페이지로 갈수록 병목 발생
    public Page<PointHistoryResponse> getPointHistory(Long userId, int page, int size) {
        return pointHistoryRepository.findByUserId(userId, PageRequest.of(page, size))
                .map(PointHistoryResponse::from);
    }
}
