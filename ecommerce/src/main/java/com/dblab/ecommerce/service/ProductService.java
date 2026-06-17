package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.dto.ProductReviewSummaryResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductReviewSummaryRepository;
import com.dblab.ecommerce.service.product.ProductSearchStrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductSearchStrategyRegistry productSearchStrategyRegistry;
    private final ProductReviewSummaryRepository productReviewSummaryRepository;

    // 인덱스 없는 상태에서 categoryId + status 필터 → Seq Scan 유발
    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        return searchProducts(categoryId, status, "querydsl");
    }

    public List<ProductResponse> searchProducts(
            Long categoryId,
            Product.Status status,
            String strategyName) {
        return productSearchStrategyRegistry.get(strategyName).search(categoryId, status);
    }

    public List<ProductReviewSummaryResponse> getReviewSummary() {
        return productReviewSummaryRepository.findTopReviewSummaries();
    }

    public List<ProductResponse> searchProducts(
            Long categoryId,
            Product.Status status,
            ProductSearchStrategyName strategyName) {
        return searchProducts(categoryId, status, strategyName.value());
    }
}
