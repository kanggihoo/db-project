package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/product-setup.sql")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private final Long categoryId = 200L;

    @Test
    void findByCategoryIdAndStatusReturnsAllOnSaleRowsIncludingDeletedFixtureRow() {
        List<Product> onSaleProducts = productRepository.findByCategoryIdAndStatus(
                categoryId, Product.Status.ON_SALE);

        assertThat(onSaleProducts).hasSize(6);
        assertThat(onSaleProducts).allMatch(product -> product.getStatus() == Product.Status.ON_SALE);
        System.out.println("Seq Scan product search ON_SALE result: " + onSaleProducts.size());
    }

    @Test
    void findByCategoryIdAndStatusReturnsSoldOutRows() {
        List<Product> soldOutProducts = productRepository.findByCategoryIdAndStatus(
                categoryId, Product.Status.SOLD_OUT);

        assertThat(soldOutProducts).hasSize(2);
        System.out.println("Seq Scan product search SOLD_OUT result: " + soldOutProducts.size());
    }
}
