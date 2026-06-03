package com.dblab.ecommerce.controller;

import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping
    public Page<PointHistoryResponse> getPointHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return pointService.getPointHistory(userId, page, size);
    }

    @GetMapping("/cursor")
    public PointHistoryCursorResponse getPointHistoryByCursor(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreatedAt,
            @RequestParam(required = false) Long lastId
    ) {
        return pointService.getPointHistoryByCursor(userId, size, lastCreatedAt, lastId);
    }
}
