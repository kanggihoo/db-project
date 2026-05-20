package com.dblab.ecommerce.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLoadingStrategyTest {

    @Test
    void 빈_전략은_lazy로_처리한다() {
        assertThat(OrderLoadingStrategy.from(null)).isEqualTo(OrderLoadingStrategy.LAZY);
        assertThat(OrderLoadingStrategy.from("")).isEqualTo(OrderLoadingStrategy.LAZY);
    }

    @Test
    void kebab_case_전략을_enum으로_변환한다() {
        assertThat(OrderLoadingStrategy.from("fetch-join")).isEqualTo(OrderLoadingStrategy.FETCH_JOIN);
        assertThat(OrderLoadingStrategy.from("batch-size")).isEqualTo(OrderLoadingStrategy.BATCH_SIZE);
        assertThat(OrderLoadingStrategy.from("entity-graph")).isEqualTo(OrderLoadingStrategy.ENTITY_GRAPH);
    }

    @Test
    void 알수없는_전략은_예외를_던진다() {
        assertThatThrownBy(() -> OrderLoadingStrategy.from("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported order loading strategy");
    }
}
