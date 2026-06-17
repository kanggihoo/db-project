package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;

import java.util.List;

public interface OrderLoadingStrategy {
    String name();

    List<OrderResponse> loadByUserId(Long userId);
}
