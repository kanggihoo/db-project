package com.dblab.ecommerce.service.product;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuerydslProductSearchStrategy implements ProductSearchStrategy {

    private final ProductQueryRepository productQueryRepository;

    @Override
    public String name() {
        return "querydsl";
    }

    @Override
    public List<ProductResponse> search(Long categoryId, Product.Status status) {
        return productQueryRepository.searchProducts(categoryId, status);
    }
}
