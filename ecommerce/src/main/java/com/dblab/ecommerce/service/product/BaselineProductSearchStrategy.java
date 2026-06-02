package com.dblab.ecommerce.service.product;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BaselineProductSearchStrategy implements ProductSearchStrategy {

    private final ProductRepository productRepository;

    @Override
    public String name() {
        return "baseline";
    }

    @Override
    public List<ProductResponse> search(Long categoryId, Product.Status status) {
        return findBaselineProducts(categoryId, status)
                .stream()
                .map(ProductResponse::from)
                .toList();
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
