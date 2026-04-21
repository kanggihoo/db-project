package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndStatus(Long categoryId, Product.Status status);
}
