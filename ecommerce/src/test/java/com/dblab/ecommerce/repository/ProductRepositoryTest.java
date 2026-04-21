package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import java.util.List;

import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/product-setup.sql")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private final Long categoryId = 200L; // SQL 파일에서 지정한 ID

    @Test
    void 카테고리와_상태로_상품_필터링_인덱스없이_전체스캔() {
        List<Product> onSaleProducts = productRepository.findByCategoryIdAndStatus(
                categoryId, Product.Status.ON_SALE);
        assertThat(onSaleProducts).hasSize(5);
        assertThat(onSaleProducts).allMatch(p -> p.getStatus() == Product.Status.ON_SALE);
        System.out.println("Seq Scan 유발 조회 — ON_SALE 결과: " + onSaleProducts.size() + "건");
    }

    @Test
    void 품절_상품_필터링_결과_건수_검증() {
        List<Product> soldOutProducts = productRepository.findByCategoryIdAndStatus(
                categoryId, Product.Status.SOLD_OUT);
        assertThat(soldOutProducts).hasSize(2);
        System.out.println("Seq Scan 유발 조회 — SOLD_OUT 결과: " + soldOutProducts.size() + "건");
    }
}
