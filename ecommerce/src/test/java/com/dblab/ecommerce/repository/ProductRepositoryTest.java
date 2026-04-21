package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO category (name, depth) VALUES (?, ?)", "전자제품", 0);
        categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = '전자제품'", Long.class);

        // ON_SALE 상품 5건
        for (int i = 0; i < 5; i++) {
            jdbcTemplate.update("""
                    INSERT INTO product (category_id, name, base_price, status, is_deleted, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    categoryId, "판매중상품" + i, 10000, "ON_SALE", false,
                    LocalDateTime.now(), LocalDateTime.now());
        }

        // SOLD_OUT 상품 2건
        for (int i = 0; i < 2; i++) {
            jdbcTemplate.update("""
                    INSERT INTO product (category_id, name, base_price, status, is_deleted, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    categoryId, "품절상품" + i, 10000, "SOLD_OUT", false,
                    LocalDateTime.now(), LocalDateTime.now());
        }

        entityManager.clear();
    }

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
