package com.dblab.ecommerce.service.product;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSearchStrategyRegistryTest {

    @Test
    @DisplayName("빈 전략명은 querydsl 전략으로 처리한다")
    void shouldResolveBlankNameToQuerydsl() {
        StubProductSearchStrategy querydsl = new StubProductSearchStrategy("querydsl");
        ProductSearchStrategyRegistry registry = new ProductSearchStrategyRegistry(List.of(querydsl));

        assertThat(registry.get(null)).isSameAs(querydsl);
        assertThat(registry.get("")).isSameAs(querydsl);
    }

    @Test
    @DisplayName("전략명 대소문자를 무시하고 등록된 전략을 찾는다")
    void shouldResolveByNameIgnoringCase() {
        StubProductSearchStrategy baseline = new StubProductSearchStrategy("baseline");
        ProductSearchStrategyRegistry registry = new ProductSearchStrategyRegistry(List.of(
                baseline,
                new StubProductSearchStrategy("querydsl")
        ));

        assertThat(registry.get("BASELINE")).isSameAs(baseline);
    }

    @Test
    @DisplayName("알 수 없는 전략명은 예외를 던진다")
    void shouldRejectUnknownName() {
        ProductSearchStrategyRegistry registry = new ProductSearchStrategyRegistry(List.of(
                new StubProductSearchStrategy("querydsl")
        ));

        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported product search strategy: unknown");
    }

    @Test
    @DisplayName("중복 전략명은 registry 생성 시점에 실패한다")
    void shouldRejectDuplicateNames() {
        assertThatThrownBy(() -> new ProductSearchStrategyRegistry(List.of(
                new StubProductSearchStrategy("querydsl"),
                new StubProductSearchStrategy("QUERYDSL")
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate product search strategy: querydsl");
    }

    private record StubProductSearchStrategy(String name) implements ProductSearchStrategy {
        @Override
        public List<ProductResponse> search(Long categoryId, Product.Status status) {
            return List.of();
        }
    }
}
