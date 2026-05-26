package com.dblab.ecommerce.controller;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.service.ProductSearchStrategy;
import com.dblab.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> searchProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Product.Status status,
            @RequestParam(defaultValue = "querydsl") String strategy) {
        ProductSearchStrategy searchStrategy = resolveStrategy(strategy);
        return productService.searchProducts(categoryId, status, searchStrategy);
    }

    private ProductSearchStrategy resolveStrategy(String strategy) {
        try {
            return ProductSearchStrategy.from(strategy);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
