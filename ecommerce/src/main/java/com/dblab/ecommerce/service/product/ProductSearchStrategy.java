package com.dblab.ecommerce.service.product;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;

import java.util.List;

public interface ProductSearchStrategy {
    String name();

    List<ProductResponse> search(Long categoryId, Product.Status status);
}
