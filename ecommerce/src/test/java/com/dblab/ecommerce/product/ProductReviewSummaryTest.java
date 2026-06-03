package com.dblab.ecommerce.product;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.dto.ProductReviewSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/phase6-review-summary-setup.sql")
class ProductReviewSummaryTest {

    @Autowired
    private RestTestClient restClient;

    @Test
    @DisplayName("review summary API returns products with at least ten reviews ordered by average rating")
    void reviewSummaryApiReturnsProductsWithAtLeastTenReviewsOrderedByAverageRating() {
        restClient.get()
                .uri("/api/products/review-summary")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductReviewSummaryResponse[].class)
                .value(response -> {
                    assertThat(response)
                            .extracting(
                                    ProductReviewSummaryResponse::productId,
                                    ProductReviewSummaryResponse::productName,
                                    ProductReviewSummaryResponse::avgRating,
                                    ProductReviewSummaryResponse::reviewCount)
                            .containsExactly(
                                    tuple(6300L, "Phase6 Product A", new BigDecimal("5.00"), 10L),
                                    tuple(6301L, "Phase6 Product B", new BigDecimal("4.00"), 10L));

                    assertThat(response)
                            .extracting(ProductReviewSummaryResponse::productId)
                            .doesNotContain(6302L);
                });
    }
}
