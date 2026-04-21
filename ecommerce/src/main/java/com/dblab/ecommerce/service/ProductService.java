package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
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

    // 인덱스 없는 상태에서 categoryId + status 필터 → Seq Scan 유발
    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        return productRepository.findByCategoryIdAndStatus(categoryId, status)
                .stream().map(ProductResponse::from).toList();
    }
}
