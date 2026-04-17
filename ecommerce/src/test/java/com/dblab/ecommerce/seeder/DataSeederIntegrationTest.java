package com.dblab.ecommerce.seeder;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.repository.BulkInsertRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * DataSeeder 통합 테스트
 *
 * 검증 목표:
 * 1. reserveSequence: 시퀀스에서 N개 ID를 정확히 확보하는지
 * 2. 각 Layer의 벌크 삽입이 실제 DB에 반영되는지
 * 3. FK 정합성: 고아 데이터가 없는지
 * 4. 데이터 분포: grade 비율이 의도대로 삽입되는지
 *
 * TDD 순서: Skeleton compile → RED → Implementation → GREEN
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataSeederIntegrationTest {

    @Autowired
    private BulkInsertRepository bulkRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ============================================================
    // Test 1: reserveSequence
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("reserveSequence는 요청한 수만큼 고유한 ID를 반환해야 한다")
    void reserveSequence_shouldReturnRequestedCountOfUniqueIds() {
        // given
        int count = 10;

        // when
        List<Long> ids = bulkRepo.reserveSequence("users_id_seq", count);

        // then
        assertThat(ids).hasSize(count);
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(ids).allMatch(id -> id > 0);
    }

    // ============================================================
    // Test 2: Layer 0 벌크 삽입 (users)
    // ============================================================

    @Test
    @Order(2)
    @DisplayName("bulkInsertUsers는 요청한 수만큼 users 테이블에 삽입해야 한다")
    void bulkInsertUsers_shouldInsertAllRows() {
        // given
        int count = 5;
        List<Long> ids = bulkRepo.reserveSequence("users_id_seq", count);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(new Object[]{
                ids.get(i),
                "test_bulk_" + ids.get(i) + "@example.com",
                "password",
                "테스트유저" + i,
                "010-0000-000" + i,
                "MALE",
                java.sql.Date.valueOf("1990-01-01"),
                "BRONZE",
                0,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now())
            });
        }

        // when
        bulkRepo.bulkInsertUsers(rows);

        // then
        String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        Long insertedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE id IN (" + inClause + ")",
            Long.class
        );
        assertThat(insertedCount).isEqualTo(count);
    }

    // ============================================================
    // Test 3: FK 정합성 (orders → user_address → order_item)
    // ============================================================

    @Test
    @Order(3)
    @DisplayName("FK 정합성: order_item의 모든 order_id는 orders에 존재해야 한다")
    void fkIntegrity_orderItem_shouldHaveValidOrderId() {
        // given: Layer 순서에 맞게 users → category → product → sku → user_address → orders → order_item 삽입

        // users 2명
        List<Long> uIds = bulkRepo.reserveSequence("users_id_seq", 2);
        List<Object[]> userRows = new ArrayList<>();
        userRows.add(new Object[]{uIds.get(0), "fk_user1@example.com", "pw", "유저1", "010-1111-1111", "MALE", java.sql.Date.valueOf("1990-01-01"), "BRONZE", 0, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())});
        userRows.add(new Object[]{uIds.get(1), "fk_user2@example.com", "pw", "유저2", "010-2222-2222", "FEMALE", java.sql.Date.valueOf("1991-01-01"), "SILVER", 0, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())});
        bulkRepo.bulkInsertUsers(userRows);

        // user_address 2개
        List<Long> aIds = bulkRepo.reserveSequence("user_address_id_seq", 2);
        List<Object[]> addrRows = new ArrayList<>();
        addrRows.add(new Object[]{aIds.get(0), uIds.get(0), "서울시 강남구", "101호", true, "유저1", "010-1111-1111"});
        addrRows.add(new Object[]{aIds.get(1), uIds.get(1), "서울시 서초구", "202호", true, "유저2", "010-2222-2222"});
        bulkRepo.bulkInsertUserAddresses(addrRows);

        // orders 2개
        List<Long> oIds = bulkRepo.reserveSequence("orders_id_seq", 2);
        List<Object[]> orderRows = new ArrayList<>();
        orderRows.add(new Object[]{oIds.get(0), uIds.get(0), aIds.get(0), null, 10000, 0, 10000, "PAID", Timestamp.valueOf(LocalDateTime.now())});
        orderRows.add(new Object[]{oIds.get(1), uIds.get(1), aIds.get(1), null, 20000, 0, 20000, "DELIVERED", Timestamp.valueOf(LocalDateTime.now())});
        bulkRepo.bulkInsertOrders(orderRows);

        // category → product → sku (order_item의 sku_id FK 충족)
        List<Long> cIds = bulkRepo.reserveSequence("category_id_seq", 1);
        List<Object[]> catRows = new ArrayList<>();
        catRows.add(new Object[]{cIds.get(0), null, "테스트카테고리", 0});
        bulkRepo.bulkInsertCategories(catRows);

        List<Long> pIds = bulkRepo.reserveSequence("product_id_seq", 1);
        List<Object[]> prodRows = new ArrayList<>();
        prodRows.add(new Object[]{pIds.get(0), cIds.get(0), "테스트상품", "설명", 10000, "ON_SALE", false, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())});
        bulkRepo.bulkInsertProducts(prodRows);

        List<Long> sIds = bulkRepo.reserveSequence("product_sku_id_seq", 1);
        List<Object[]> skuRows = new ArrayList<>();
        skuRows.add(new Object[]{sIds.get(0), pIds.get(0), "TEST-SKU-FK", 100, 0});
        bulkRepo.bulkInsertProductSkus(skuRows);

        // order_item 2개
        List<Long> oiIds = bulkRepo.reserveSequence("order_item_id_seq", 2);
        List<Object[]> oiRows = new ArrayList<>();
        oiRows.add(new Object[]{oiIds.get(0), oIds.get(0), sIds.get(0), "테스트상품", "색상:빨강", 1, 10000, "DELIVERED"});
        oiRows.add(new Object[]{oiIds.get(1), oIds.get(1), sIds.get(0), "테스트상품", "색상:파랑", 2, 20000, "DELIVERED"});
        bulkRepo.bulkInsertOrderItems(oiRows);

        // when: 고아 데이터 확인
        Long orphanCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM order_item oi LEFT JOIN orders o ON o.id = oi.order_id WHERE o.id IS NULL",
            Long.class
        );

        // then
        assertThat(orphanCount).isZero();
    }

    // ============================================================
    // Test 4: 데이터 분포 검증
    // ============================================================

    @Test
    @Order(4)
    @DisplayName("users의 grade 분포는 BRONZE가 가장 많아야 한다")
    void userGradeDistribution_shouldHaveMostlyBronze() {
        // given: 100명의 users를 grade 비율에 맞게 삽입
        int total = 100;
        List<Long> ids = bulkRepo.reserveSequence("users_id_seq", total);
        List<Object[]> rows = new ArrayList<>();

        // BRONZE 60%, SILVER 25%, GOLD 10%, VIP 5%
        String[] grades = {"BRONZE", "SILVER", "GOLD", "VIP"};
        int[] counts = {60, 25, 10, 5};
        int idx = 0;
        for (int g = 0; g < grades.length; g++) {
            for (int c = 0; c < counts[g]; c++) {
                long id = ids.get(idx);
                rows.add(new Object[]{
                    id, "dist_" + id + "@example.com", "pw", "유저" + idx, "010-0000-0000",
                    "MALE", java.sql.Date.valueOf("1990-01-01"), grades[g], 0,
                    Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())
                });
                idx++;
            }
        }

        // when
        bulkRepo.bulkInsertUsers(rows);

        // then: BRONZE 수가 SILVER, GOLD보다 많아야 함
        String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<String, Long> gradeCounts = new HashMap<>();
        jdbcTemplate.query(
            "SELECT grade, COUNT(*) as cnt FROM users WHERE id IN (" + inClause + ") GROUP BY grade",
            (RowCallbackHandler) rs -> gradeCounts.put(rs.getString("grade"), rs.getLong("cnt"))
        );

        assertThat(gradeCounts.get("BRONZE")).isGreaterThan(gradeCounts.getOrDefault("SILVER", 0L));
        assertThat(gradeCounts.get("BRONZE")).isGreaterThan(gradeCounts.getOrDefault("GOLD", 0L));
    }
}
