package com.dblab.ecommerce.product;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.config.QuerydslConfig;
import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductQueryRepository;
import com.dblab.ecommerce.repository.ProductRepository;
import com.dblab.ecommerce.service.ProductSearchStrategy;
import com.dblab.ecommerce.service.ProductService;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        TestcontainersConfiguration.class,
        QuerydslConfig.class,
        ProductQueryRepository.class,
        ProductService.class
})
@Sql("/test-data/product-setup.sql")
class ProductSearchStrategyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("baseline and querydsl return the same responses for shared category and status conditions")
    void baselineAndQuerydslReturnSameResponsesForSharedConditions() {
        List<ProductResponse> baseline = productService.searchProducts(
                200L, Product.Status.ON_SALE, ProductSearchStrategy.BASELINE);
        List<ProductResponse> querydsl = productService.searchProducts(
                200L, Product.Status.ON_SALE, ProductSearchStrategy.QUERYDSL);

        assertThat(sorted(querydsl)).isEqualTo(sorted(baseline));
    }

    @Test
    @DisplayName("omitted strategy uses the same behavior as explicit querydsl")
    void omittedStrategyDefaultsToQuerydsl() {
        List<ProductResponse> omitted = productService.searchProducts(200L, Product.Status.SOLD_OUT);
        List<ProductResponse> querydsl = productService.searchProducts(
                200L, Product.Status.SOLD_OUT, ProductSearchStrategy.QUERYDSL);

        assertThat(sorted(omitted)).isEqualTo(sorted(querydsl));
    }

    @Test
    @DisplayName("two-argument overload delegates to querydsl repository")
    void twoArgumentOverloadDelegatesToQuerydslRepository() {
        ProductRepository baselineRepository = mock(ProductRepository.class);
        ProductQueryRepository queryRepository = mock(ProductQueryRepository.class);
        ProductService service = new ProductService(baselineRepository, queryRepository);
        List<ProductResponse> expected = List.of(
                new ProductResponse(205L, 200L, "Sold Out Product 0", 10000, Product.Status.SOLD_OUT));
        when(queryRepository.searchProducts(200L, Product.Status.SOLD_OUT)).thenReturn(expected);

        List<ProductResponse> result = service.searchProducts(200L, Product.Status.SOLD_OUT);

        assertThat(result).isEqualTo(expected);
        verify(queryRepository).searchProducts(200L, Product.Status.SOLD_OUT);
        verifyNoMoreInteractions(queryRepository);
        verifyNoInteractions(baselineRepository);
    }

    @Test
    @DisplayName("querydsl omits null category predicate and filters by status only")
    void querydslOmitsNullCategoryCondition() {
        List<ProductResponse> result = productService.searchProducts(
                null, Product.Status.SOLD_OUT, ProductSearchStrategy.QUERYDSL);

        assertThat(sorted(result))
                .extracting(ProductResponse::productId, ProductResponse::status)
                .containsExactly(
                        tuple(205L, Product.Status.SOLD_OUT),
                        tuple(206L, Product.Status.SOLD_OUT),
                        tuple(209L, Product.Status.SOLD_OUT));
    }

    @Test
    @DisplayName("querydsl omits null status predicate and filters by category only")
    void querydslOmitsNullStatusCondition() {
        List<ProductResponse> result = productService.searchProducts(
                201L, null, ProductSearchStrategy.QUERYDSL);

        assertThat(sorted(result))
                .extracting(ProductResponse::productId, ProductResponse::status)
                .containsExactly(
                        tuple(207L, Product.Status.ON_SALE),
                        tuple(209L, Product.Status.SOLD_OUT));
        assertThat(result).allMatch(product -> product.categoryId().equals(201L));
    }

    @Test
    @DisplayName("querydsl with no filters returns all fixture rows")
    void querydslWithNoFiltersReturnsAllFixtureRows() {
        List<ProductResponse> result = productService.searchProducts(
                null, null, ProductSearchStrategy.QUERYDSL);

        assertThat(sorted(result))
                .extracting(ProductResponse::productId)
                .containsExactly(200L, 201L, 202L, 203L, 204L, 205L, 206L, 207L, 208L, 209L);
    }

    @Test
    @DisplayName("baseline and querydsl each use one SQL statement for shared conditions")
    void baselineAndQuerydslEachUseSingleStatementForSharedConditions() {
        Statistics statistics = statistics();

        statistics.clear();
        productService.searchProducts(200L, Product.Status.ON_SALE, ProductSearchStrategy.BASELINE);
        long baselineStatements = statistics.getPrepareStatementCount();

        statistics.clear();
        productService.searchProducts(200L, Product.Status.ON_SALE, ProductSearchStrategy.QUERYDSL);
        long querydslStatements = statistics.getPrepareStatementCount();

        assertThat(baselineStatements).isEqualTo(1);
        assertThat(querydslStatements).isEqualTo(1);

        System.out.println("PHASE5_PRODUCT_SEARCH_BASELINE_SQL_COUNT=" + baselineStatements);
        System.out.println("PHASE5_PRODUCT_SEARCH_QUERYDSL_SQL_COUNT=" + querydslStatements);
    }

    private Statistics statistics() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }

    private List<ProductResponse> sorted(List<ProductResponse> responses) {
        return responses.stream()
                .sorted(Comparator.comparing(ProductResponse::productId))
                .toList();
    }
}
