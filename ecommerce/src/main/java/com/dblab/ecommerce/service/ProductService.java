package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductQueryRepository;
import com.dblab.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;

    // 인덱스 없는 상태에서 categoryId + status 필터 → Seq Scan 유발
    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        return searchProducts(categoryId, status, ProductSearchStrategy.QUERYDSL);
    }

    public List<ProductResponse> searchProducts(
            Long categoryId,
            Product.Status status,
            ProductSearchStrategy strategy) {
        return switch (strategy) {
            case BASELINE -> searchProductsBaseline(categoryId, status);
            case QUERYDSL -> productQueryRepository.searchProducts(categoryId, status);
        };
    }

    private List<ProductResponse> searchProductsBaseline(Long categoryId, Product.Status status) {
        return findBaselineProducts(categoryId, status)
                .stream().map(ProductResponse::from).toList();
    }

    private List<Product> findBaselineProducts(Long categoryId, Product.Status status) {
        if (categoryId != null && status != null) {
            return productRepository.findByCategoryIdAndStatus(categoryId, status);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId);
        }
        if (status != null) {
            return productRepository.findByStatus(status);
        }
        return productRepository.findAll();
    }
}
