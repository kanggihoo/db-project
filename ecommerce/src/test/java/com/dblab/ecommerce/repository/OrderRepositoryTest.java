package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.entity.Orders;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
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
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long savedUserId;
    private Long savedAddressId;

    @BeforeEach
    void setUp() {
        // users 테이블에 직접 삽입
        jdbcTemplate.update("""
                INSERT INTO users (email, password, name, gender, grade, point_balance, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "test@test.com", "pw", "홍길동", "MALE", "BRONZE", 0,
                LocalDateTime.now(), LocalDateTime.now());
        savedUserId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'test@test.com'", Long.class);

        // user_address 삽입
        jdbcTemplate.update("""
                INSERT INTO user_address (user_id, address, detail_address, is_default, receiver_name, receiver_phone)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                savedUserId, "서울시 강남구", "101호", true, "홍길동", "010-0000-0000");
        savedAddressId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_address WHERE user_id = ?", Long.class, savedUserId);

        // category 삽입
        jdbcTemplate.update("INSERT INTO category (name, depth) VALUES (?, ?)", "테스트카테고리", 0);
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM category WHERE name = '테스트카테고리'", Long.class);

        // product 삽입
        jdbcTemplate.update("""
                INSERT INTO product (category_id, name, base_price, status, is_deleted, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                categoryId, "테스트상품", 10000, "ON_SALE", false,
                LocalDateTime.now(), LocalDateTime.now());
        Long productId = jdbcTemplate.queryForObject(
                "SELECT id FROM product WHERE name = '테스트상품'", Long.class);

        // product_sku 삽입
        jdbcTemplate.update("""
                INSERT INTO product_sku (product_id, sku_code, stock_quantity, extra_price)
                VALUES (?, ?, ?, ?)
                """,
                productId, "SKU-001", 100, 0);
        Long skuId = jdbcTemplate.queryForObject(
                "SELECT id FROM product_sku WHERE sku_code = 'SKU-001'", Long.class);

        // 주문 3건 삽입
        for (int i = 0; i < 3; i++) {
            jdbcTemplate.update(
                    """
                            INSERT INTO orders (user_id, address_id, total_price, discount_price, final_price, status, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    savedUserId, savedAddressId, 10000, 0, 10000, "PENDING", LocalDateTime.now());
        }

        // 각 주문마다 order_item 삽입
        List<Long> orderIds = jdbcTemplate.queryForList(
                "SELECT id FROM orders WHERE user_id = ?", Long.class, savedUserId);
        for (Long orderId : orderIds) {
            jdbcTemplate.update("""
                    INSERT INTO order_item (order_id, sku_id, product_name, quantity, unit_price, status)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    orderId, skuId, "테스트상품", 1, 10000, "PENDING");
        }

        entityManager.clear();
    }

    @Test
    void 주문_목록_조회_후_OrderItem_루프_접근시_N개_추가쿼리_발생() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Orders> orders = orderRepository.findByUserId(savedUserId);
        assertThat(orders).hasSize(3);

        long sqlCountBeforeLoop = statistics.getPrepareStatementCount();

        // N+1 기폭제: 주문마다 OrderItem을 별도 쿼리로 조회
        for (Orders order : orders) {
            orderItemRepository.findByOrderId(order.getId());
        }

        long extraSqlCount = statistics.getPrepareStatementCount() - sqlCountBeforeLoop;

        assertThat(extraSqlCount).isEqualTo(orders.size());
        System.out.println("N+1 발생 확인 — 루프 추가 SQL 수: " + extraSqlCount);
    }
}
