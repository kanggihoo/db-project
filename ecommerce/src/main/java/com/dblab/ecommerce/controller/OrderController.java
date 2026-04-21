package com.dblab.ecommerce.controller;

import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderResponse> getOrders(@RequestParam Long userId) {
        return orderService.getOrdersByUserId(userId);
    }
}
