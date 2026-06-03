# 001 Baseline And Naming Contract

## Goal

현재 behavior를 focused tests로 확인하고, 기존 enum을 `*StrategyName` compatibility 타입으로 rename할 기준을 고정한다.

## Files

- Rename: `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderLoadingStrategy.java` -> `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderLoadingStrategyName.java`
- Rename: `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategy.java` -> `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategyName.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`
- Rename: `ecommerce/src/test/java/com/dblab/ecommerce/service/OrderLoadingStrategyTest.java` -> `ecommerce/src/test/java/com/dblab/ecommerce/service/OrderLoadingStrategyNameTest.java`
- Create: `ecommerce/src/test/java/com/dblab/ecommerce/service/ProductSearchStrategyNameTest.java`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java`

## Steps

- [ ] **Step 1: Confirm working tree scope**

Run:

```bash
rtk git status --short --branch
```

Expected:

- branch is `codex/refactor-tooling`
- root reference specs may appear untracked and must not be staged
- only query strategy spec/plan files should be untracked before implementation begins

- [ ] **Step 2: Run current focused tests before refactor**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*OrderLoadingStrategyTest" --tests "*ProductSearchStrategyTest" --tests "*PointCursorApiTest"
```

Expected:

- command exits 0
- this establishes behavior before renaming and strategy extraction

- [ ] **Step 3: Rename `OrderLoadingStrategy` enum to `OrderLoadingStrategyName`**

Move file:

```bash
git mv ecommerce/src/main/java/com/dblab/ecommerce/service/OrderLoadingStrategy.java ecommerce/src/main/java/com/dblab/ecommerce/service/OrderLoadingStrategyName.java
```

Replace the enum declaration with:

```java
package com.dblab.ecommerce.service;

import java.util.Arrays;

public enum OrderLoadingStrategyName {
    LAZY("lazy"),
    FETCH_JOIN("fetch-join"),
    BATCH_SIZE("batch-size"),
    ENTITY_GRAPH("entity-graph");

    private final String value;

    OrderLoadingStrategyName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OrderLoadingStrategyName from(String value) {
        if (value == null || value.isBlank()) {
            return LAZY;
        }
        return Arrays.stream(values())
                .filter(strategy -> strategy.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported order loading strategy: " + value));
    }
}
```

- [ ] **Step 4: Rename `ProductSearchStrategy` enum to `ProductSearchStrategyName`**

Move file:

```bash
git mv ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategy.java ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategyName.java
```

Replace the enum declaration with:

```java
package com.dblab.ecommerce.service;

import java.util.Arrays;

public enum ProductSearchStrategyName {
    BASELINE("baseline"),
    QUERYDSL("querydsl");

    private final String value;

    ProductSearchStrategyName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ProductSearchStrategyName from(String value) {
        if (value == null || value.isBlank()) {
            return QUERYDSL;
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(strategy -> strategy.value.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported product search strategy: " + value));
    }
}
```

- [ ] **Step 5: Update imports and references**

Replace:

```java
OrderLoadingStrategy
```

with:

```java
OrderLoadingStrategyName
```

in:

- `ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java`
- `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java`
- `ecommerce/src/test/java/com/dblab/ecommerce/service/OrderLoadingStrategyNameTest.java`

Replace:

```java
ProductSearchStrategy
```

with:

```java
ProductSearchStrategyName
```

in:

- `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`
- `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`
- `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java`

- [ ] **Step 6: Rename and update order enum test**

Move file:

```bash
git mv ecommerce/src/test/java/com/dblab/ecommerce/service/OrderLoadingStrategyTest.java ecommerce/src/test/java/com/dblab/ecommerce/service/OrderLoadingStrategyNameTest.java
```

Replace class content with:

```java
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
```

- [ ] **Step 7: Run renamed focused tests**

Create `ecommerce/src/test/java/com/dblab/ecommerce/service/ProductSearchStrategyNameTest.java`:

```java
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
```

- [ ] **Step 8: Run renamed focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*OrderLoadingStrategyNameTest" --tests "*ProductSearchStrategyNameTest" --tests "*ProductSearchStrategyTest"
```

Expected:

- command exits 0
- only rename/reference updates are included in this slice

- [ ] **Step 9: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/service ecommerce/src/main/java/com/dblab/ecommerce/controller ecommerce/src/test/java/com/dblab/ecommerce/service ecommerce/src/test/java/com/dblab/ecommerce/product
git commit -m "refactor(query): rename strategy enums as request names"
```
