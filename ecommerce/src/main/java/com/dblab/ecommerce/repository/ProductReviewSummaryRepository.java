package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.dto.ProductReviewSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class ProductReviewSummaryRepository {

    private static final String REVIEW_SUMMARY_SQL = """
            SELECT p.id AS product_id,
                   p.name AS product_name,
                   ROUND(AVG(r.rating), 2) AS avg_rating,
                   COUNT(r.id) AS review_count
            FROM product p
            LEFT JOIN review r ON r.product_id = p.id
            GROUP BY p.id, p.name
            HAVING COUNT(r.id) >= 10
            ORDER BY AVG(r.rating) DESC, p.id ASC
            LIMIT 100
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProductReviewSummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductReviewSummaryResponse> findTopReviewSummaries() {
        return jdbcTemplate.query(REVIEW_SUMMARY_SQL, (rs, rowNum) -> new ProductReviewSummaryResponse(
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getObject("avg_rating", BigDecimal.class),
                rs.getLong("review_count")));
    }
}
