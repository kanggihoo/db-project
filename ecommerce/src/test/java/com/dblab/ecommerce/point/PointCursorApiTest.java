package com.dblab.ecommerce.point;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/point-history-cursor-setup.sql")
class PointCursorApiTest {

    @Autowired
    private RestTestClient restClient;

    @Test
    @DisplayName("offset API uses createdAt and id descending order")
    void offsetApiUsesCreatedAtAndIdDescendingOrder() {
        restClient.get()
                .uri("/api/points?userId=7300&page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"id\":7302");
                    assertThat(body).contains("\"id\":7301");
                    assertThat(body.indexOf("\"id\":7302"))
                            .isLessThan(body.indexOf("\"id\":7301"));
                });
    }

    @Test
    @DisplayName("cursor API returns first page and next cursor")
    void cursorApiReturnsFirstPageAndNextCursor() {
        PointHistoryCursorResponse response = restClient.get()
                .uri("/api/points/cursor?userId=7300&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PointHistoryCursorResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
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
        PointHistoryCursorResponse first = restClient.get()
                .uri("/api/points/cursor?userId=7300&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PointHistoryCursorResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(first).isNotNull();
        PointHistoryCursorResponse second = restClient.get()
                .uri("/api/points/cursor?userId=7300&size=2&lastCreatedAt={lastCreatedAt}&lastId={lastId}",
                        first.nextCursor().lastCreatedAt(),
                        first.nextCursor().lastId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PointHistoryCursorResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(second).isNotNull();
        assertThat(second.items())
                .extracting("id")
                .containsExactly(7303L, 7304L);
        assertThat(second.hasNext()).isTrue();
    }
}
