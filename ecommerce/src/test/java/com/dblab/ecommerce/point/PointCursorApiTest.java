package com.dblab.ecommerce.point;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/point-history-cursor-setup.sql")
class PointCursorApiTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("cursor API returns first page and next cursor")
    void cursorApiReturnsFirstPageAndNextCursor() {
        PointHistoryCursorResponse response = restTemplate.getForObject(
                "/api/points/cursor?userId=7300&size=2",
                PointHistoryCursorResponse.class);

        assertThat(response.items()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isNotNull();
        assertThat(response.items())
                .extracting("id")
                .containsExactly(7302L, 7301L);
    }

    @Test
    @DisplayName("cursor API returns rows after supplied cursor")
    void cursorApiReturnsRowsAfterSuppliedCursor() {
        PointHistoryCursorResponse first = restTemplate.getForObject(
                "/api/points/cursor?userId=7300&size=2",
                PointHistoryCursorResponse.class);

        PointHistoryCursorResponse second = restTemplate.getForObject(
                "/api/points/cursor?userId=7300&size=2&lastCreatedAt={lastCreatedAt}&lastId={lastId}",
                PointHistoryCursorResponse.class,
                first.nextCursor().lastCreatedAt(),
                first.nextCursor().lastId());

        assertThat(second.items())
                .extracting("id")
                .containsExactly(7303L, 7304L);
        assertThat(second.hasNext()).isTrue();
    }
}
