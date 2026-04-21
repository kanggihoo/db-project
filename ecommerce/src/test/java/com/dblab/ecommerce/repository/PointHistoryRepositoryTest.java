package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.entity.PointHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/point-history-setup.sql")
class PointHistoryRepositoryTest {

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    private final Long savedUserId = 300L; // SQL 파일에서 지정한 ID

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
