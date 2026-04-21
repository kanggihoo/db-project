package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.entity.PointHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PointHistoryRepositoryTest {

    @Autowired
    private PointHistoryRepository pointHistoryRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long savedUserId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                INSERT INTO users (email, password, name, gender, grade, point_balance, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "point@test.com", "pw", "포인트유저", "FEMALE", "GOLD", 5000,
                LocalDateTime.now(), LocalDateTime.now());
        savedUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'point@test.com'", Long.class);

        // 포인트 이력 25건 삽입
        for (int i = 0; i < 25; i++) {
            jdbcTemplate.update("""
                    INSERT INTO point_history (user_id, type, amount, balance_after, description, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    savedUserId, "EARN", 100, 100 * (i + 1), "테스트 적립" + i, LocalDateTime.now());
        }

        entityManager.clear();
    }

    @Test
    void 첫번째_페이지_10건_반환() {
        Page<PointHistory> page = pointHistoryRepository.findByUserId(
                savedUserId, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
        System.out.println("Offset 페이징 1페이지 — 반환: " + page.getContent().size() + "건");
    }

    @Test
    void 마지막_페이지_나머지_건수_반환() {
        Page<PointHistory> page = pointHistoryRepository.findByUserId(
                savedUserId, PageRequest.of(2, 10));
        assertThat(page.getContent()).hasSize(5);
        System.out.println("Offset 페이징 3페이지 — 반환: " + page.getContent().size() + "건");
    }

    @Test
    void 존재하지_않는_페이지_빈_결과_반환() {
        Page<PointHistory> page = pointHistoryRepository.findByUserId(
                savedUserId, PageRequest.of(100, 10));
        assertThat(page.getContent()).isEmpty();
        System.out.println("Offset 페이징 100페이지 — 빈 결과 확인");
    }
}
