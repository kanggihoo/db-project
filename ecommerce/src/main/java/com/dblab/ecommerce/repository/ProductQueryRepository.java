package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.entity.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        QProduct product = QProduct.product;
        BooleanBuilder builder = new BooleanBuilder();

        if (categoryId != null) {
            builder.and(product.categoryId.eq(categoryId));
        }
        if (status != null) {
            builder.and(product.status.eq(status));
        }

        return queryFactory
                .select(Projections.constructor(
                        ProductResponse.class,
                        product.id,
                        product.categoryId,
                        product.name,
                        product.basePrice,
                        product.status))
                .from(product)
                .where(builder)
                .fetch();
    }
}
