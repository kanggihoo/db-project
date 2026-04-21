package com.dblab.ecommerce.controller;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> searchProducts(
            @RequestParam Long categoryId,
            @RequestParam Product.Status status) {
        return productService.searchProducts(categoryId, status);
    }
}
