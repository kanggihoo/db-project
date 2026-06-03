package com.dblab.ecommerce.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSearchStrategyNameTest {

    @Test
    @DisplayName("빈 전략은 querydsl로 처리한다")
    void shouldUseQuerydslWhenStrategyIsBlank() {
        assertThat(ProductSearchStrategyName.from(null)).isEqualTo(ProductSearchStrategyName.QUERYDSL);
        assertThat(ProductSearchStrategyName.from("")).isEqualTo(ProductSearchStrategyName.QUERYDSL);
    }

    @Test
    @DisplayName("상품 검색 전략명을 대소문자 무시하고 enum으로 변환한다")
    void shouldParseStrategyNamesIgnoringCase() {
        assertThat(ProductSearchStrategyName.from("baseline")).isEqualTo(ProductSearchStrategyName.BASELINE);
        assertThat(ProductSearchStrategyName.from("QUERYDSL")).isEqualTo(ProductSearchStrategyName.QUERYDSL);
    }

    @Test
    @DisplayName("알 수 없는 전략은 예외를 던진다")
    void shouldThrowExceptionForUnsupportedStrategy() {
        assertThatThrownBy(() -> ProductSearchStrategyName.from("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported product search strategy");
    }
}
