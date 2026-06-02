package com.dblab.ecommerce.service.order;

import com.dblab.ecommerce.dto.OrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderLoadingStrategyRegistryTest {

    @Test
    @DisplayName("빈 전략명은 lazy 전략으로 처리한다")
    void shouldResolveBlankNameToLazy() {
        StubOrderLoadingStrategy lazy = new StubOrderLoadingStrategy("lazy");
        OrderLoadingStrategyRegistry registry = new OrderLoadingStrategyRegistry(List.of(lazy));

        assertThat(registry.get(null)).isSameAs(lazy);
        assertThat(registry.get("")).isSameAs(lazy);
    }

    @Test
    @DisplayName("전략명으로 등록된 전략을 찾는다")
    void shouldResolveByName() {
        StubOrderLoadingStrategy fetchJoin = new StubOrderLoadingStrategy("fetch-join");
        OrderLoadingStrategyRegistry registry = new OrderLoadingStrategyRegistry(List.of(
                new StubOrderLoadingStrategy("lazy"),
                fetchJoin
        ));

        assertThat(registry.get("fetch-join")).isSameAs(fetchJoin);
    }

    @Test
    @DisplayName("알 수 없는 전략명은 예외를 던진다")
    void shouldRejectUnknownName() {
        OrderLoadingStrategyRegistry registry = new OrderLoadingStrategyRegistry(List.of(
                new StubOrderLoadingStrategy("lazy")
        ));

        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported order loading strategy: unknown");
    }

    @Test
    @DisplayName("중복 전략명은 registry 생성 시점에 실패한다")
    void shouldRejectDuplicateNames() {
        assertThatThrownBy(() -> new OrderLoadingStrategyRegistry(List.of(
                new StubOrderLoadingStrategy("lazy"),
                new StubOrderLoadingStrategy("lazy")
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate order loading strategy: lazy");
    }

    private record StubOrderLoadingStrategy(String name) implements OrderLoadingStrategy {
        @Override
        public List<OrderResponse> loadByUserId(Long userId) {
            return List.of();
        }
    }
}
