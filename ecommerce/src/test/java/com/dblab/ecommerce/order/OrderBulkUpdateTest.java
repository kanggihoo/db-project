package com.dblab.ecommerce.order;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.entity.Orders;
import com.dblab.ecommerce.repository.OrderRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/order-test-setup.sql")
class OrderBulkUpdateTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Row-by-row dirty checking issues one update per order")
    void rowByRowUpdateIssuesOneUpdatePerOrder() {
        Statistics statistics = hibernateStatistics();
        statistics.clear();

        List<Orders> orders = orderRepository.findByStatus(Orders.Status.PENDING);
        orders.forEach(Orders::markPreparing);
        entityManager.flush();

        long sqlCount = statistics.getPrepareStatementCount();

        assertThat(orders).hasSize(3);
        assertThat(statistics.getEntityUpdateCount()).isEqualTo(3);
        assertThat(sqlCount).isEqualTo(orders.size() + 1);
        System.out.println("PHASE5_ROW_BY_ROW_UPDATE_SQL_COUNT=" + sqlCount);
    }

    @Test
    @DisplayName("JPQL bulk update uses one update statement")
    void bulkUpdateUsesOneUpdateStatement() {
        Statistics statistics = hibernateStatistics();
        statistics.clear();

        int updatedRows = orderRepository.bulkUpdateStatus(
                Orders.Status.PENDING,
                Orders.Status.PREPARING
        );
        long sqlCount = statistics.getPrepareStatementCount();

        assertThat(updatedRows).isEqualTo(3);
        assertThat(sqlCount).isEqualTo(1);
        assertThat(orderRepository.findByStatus(Orders.Status.PREPARING)).hasSize(3);
        System.out.println("PHASE5_BULK_UPDATE_SQL_COUNT=" + sqlCount);
    }

    @Test
    @DisplayName("JPQL bulk update clears persistence context before reload")
    void bulkUpdateClearsPersistenceContext() {
        Orders loaded = orderRepository.findById(100L).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(Orders.Status.PENDING);

        int updatedRows = orderRepository.bulkUpdateStatus(
                Orders.Status.PENDING,
                Orders.Status.PREPARING
        );

        Orders reloaded = orderRepository.findById(100L).orElseThrow();

        assertThat(updatedRows).isEqualTo(3);
        assertThat(reloaded.getStatus()).isEqualTo(Orders.Status.PREPARING);
    }

    private Statistics hibernateStatistics() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }
}
