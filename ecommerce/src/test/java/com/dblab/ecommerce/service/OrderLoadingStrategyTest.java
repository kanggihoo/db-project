package com.dblab.ecommerce.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLoadingStrategyTest {

    @Test
    @DisplayName("빈 전략 값은 lazy 전략으로 처리한다")
    void defaultsBlankStrategyToLazy() {
        assertThat(OrderLoadingStrategy.from(null)).isEqualTo(OrderLoadingStrategy.LAZY);
        assertThat(OrderLoadingStrategy.from("")).isEqualTo(OrderLoadingStrategy.LAZY);
    }

    @Test
    @DisplayName("kebab-case 전략 값을 enum으로 변환한다")
    void parsesKebabCaseStrategyValues() {
        assertThat(OrderLoadingStrategy.from("fetch-join")).isEqualTo(OrderLoadingStrategy.FETCH_JOIN);
        assertThat(OrderLoadingStrategy.from("batch-size")).isEqualTo(OrderLoadingStrategy.BATCH_SIZE);
        assertThat(OrderLoadingStrategy.from("entity-graph")).isEqualTo(OrderLoadingStrategy.ENTITY_GRAPH);
    }

    @Test
    @DisplayName("지원하지 않는 전략 값은 예외를 던진다")
    void throwsExceptionForUnsupportedStrategy() {
        assertThatThrownBy(() -> OrderLoadingStrategy.from("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported order loading strategy");
    }
}
