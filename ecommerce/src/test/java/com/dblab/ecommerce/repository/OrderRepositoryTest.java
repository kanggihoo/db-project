package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.dto.OrderResponse;
import com.dblab.ecommerce.entity.OrderItem;
import com.dblab.ecommerce.entity.Orders;
import com.dblab.ecommerce.entity.ProductImage;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import jakarta.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/order-test-setup.sql")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private EntityManager entityManager;

    private final Long savedUserId = 100L; // SQL 파일에서 지정한 고정 ID

    @Test
    void orderItemLookupPerOrderAddsOneSqlPerOrder() {
        // Given
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // When
        List<Orders> orders = orderRepository.findByUserId(savedUserId);
        assertThat(orders).hasSize(3);

        long sqlCountBeforeLoop = statistics.getPrepareStatementCount();

        // N+1 발생 유도
        for (Orders order : orders) {
            orderItemRepository.findByOrderId(order.getId());
        }

        // Then
        long extraSqlCount = statistics.getPrepareStatementCount() - sqlCountBeforeLoop;

        assertThat(extraSqlCount).isEqualTo(orders.size());
        System.out.println("[@Sql 버전] N+1 발생 확인 — 루프 추가 SQL 수: " + extraSqlCount);
    }

    @Test
    void traversesLazyAssociationPathFromOrderItemToProductImages() {
        List<Orders> orders = orderRepository.findByUserId(savedUserId);
        assertThat(orders).hasSize(3);

        OrderItem firstItem = orders.getFirst().getOrderItems().getFirst();

        assertThat(firstItem.getProductSku().getId()).isEqualTo(100L);
        assertThat(firstItem.getProductSku().getProduct().getId()).isEqualTo(100L);
        assertThat(firstItem.getProductSku().getProduct().getImages())
                .extracting(ProductImage::getImageUrl)
                .contains("https://example.com/product-100-main.jpg");

        OrderResponse response = OrderResponse.of(orders.getFirst(), orders.getFirst().getOrderItems());

        assertThat(response.items().getFirst().thumbnailUrl())
                .isEqualTo("https://example.com/product-100-main.jpg");
    }

    @Test
    void lazyAssociationTraversalAddsSqlForOrderItemsSkuProductAndImages() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Orders> orders = orderRepository.findByUserId(savedUserId);
        long beforeTraversal = statistics.getPrepareStatementCount();

        orders.forEach(order -> order.getOrderItems().forEach(item -> {
            item.getProductSku().getProduct().getImages().forEach(ProductImage::getImageUrl);
        }));

        long extraSqlCount = statistics.getPrepareStatementCount() - beforeTraversal;
        assertThat(extraSqlCount).isGreaterThanOrEqualTo(4);
    }
}
