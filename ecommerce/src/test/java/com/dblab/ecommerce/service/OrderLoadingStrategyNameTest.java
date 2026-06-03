package com.dblab.ecommerce.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLoadingStrategyNameTest {

    @Test
    @DisplayName("빈 전략은 lazy로 처리한다")
    void shouldUseLazyWhenStrategyIsBlank() {
        assertThat(OrderLoadingStrategyName.from(null)).isEqualTo(OrderLoadingStrategyName.LAZY);
        assertThat(OrderLoadingStrategyName.from("")).isEqualTo(OrderLoadingStrategyName.LAZY);
    }

    @Test
    @DisplayName("kebab-case 전략을 enum으로 변환한다")
    void shouldParseKebabCaseStrategies() {
        assertThat(OrderLoadingStrategyName.from("fetch-join")).isEqualTo(OrderLoadingStrategyName.FETCH_JOIN);
        assertThat(OrderLoadingStrategyName.from("batch-size")).isEqualTo(OrderLoadingStrategyName.BATCH_SIZE);
        assertThat(OrderLoadingStrategyName.from("entity-graph")).isEqualTo(OrderLoadingStrategyName.ENTITY_GRAPH);
    }

    @Test
    @DisplayName("알 수 없는 전략은 예외를 던진다")
    void shouldThrowExceptionForUnsupportedStrategy() {
        assertThatThrownBy(() -> OrderLoadingStrategyName.from("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported order loading strategy");
    }
}
