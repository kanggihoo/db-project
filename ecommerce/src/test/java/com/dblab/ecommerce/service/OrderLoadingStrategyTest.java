package com.dblab.ecommerce.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLoadingStrategyTest {

    @Test
    @DisplayName("빈 전략은 lazy로 처리한다")
    void shouldUseLazyWhenStrategyIsBlank() {
        assertThat(OrderLoadingStrategy.from(null)).isEqualTo(OrderLoadingStrategy.LAZY);
        assertThat(OrderLoadingStrategy.from("")).isEqualTo(OrderLoadingStrategy.LAZY);
    }

    @Test
    @DisplayName("kebab-case 전략을 enum으로 변환한다")
    void shouldParseKebabCaseStrategies() {
        assertThat(OrderLoadingStrategy.from("fetch-join")).isEqualTo(OrderLoadingStrategy.FETCH_JOIN);
        assertThat(OrderLoadingStrategy.from("batch-size")).isEqualTo(OrderLoadingStrategy.BATCH_SIZE);
        assertThat(OrderLoadingStrategy.from("entity-graph")).isEqualTo(OrderLoadingStrategy.ENTITY_GRAPH);
    }

    @Test
    @DisplayName("알 수 없는 전략은 예외를 던진다")
    void shouldThrowExceptionForUnsupportedStrategy() {
        assertThatThrownBy(() -> OrderLoadingStrategy.from("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported order loading strategy");
    }
}
