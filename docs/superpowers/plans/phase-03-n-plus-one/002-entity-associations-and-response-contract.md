# 002 Entity Associations And Response Contract

## Goal

Add lazy JPA associations for the Phase 3 order-list thumbnail path and extend the order response with a stable thumbnail field.

## Files

- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/entity/Orders.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/entity/OrderItem.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/entity/ProductSku.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/entity/Product.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/entity/ProductImage.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/dto/OrderResponse.java`
- Modify: `ecommerce/src/test/resources/test-data/order-test-setup.sql`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java`

## Steps

- [ ] **Step 1: Extend test fixture with product image data**

In `ecommerce/src/test/resources/test-data/order-test-setup.sql`, add product images after the product insert:

```sql
-- 4-1. 상품 이미지
INSERT INTO product_image (id, product_id, image_url, is_main, sort_order)
VALUES (100, 100, 'https://example.com/product-100-main.jpg', true, 1);
INSERT INTO product_image (id, product_id, image_url, is_main, sort_order)
VALUES (101, 100, 'https://example.com/product-100-sub.jpg', false, 2);
```

Expected: the existing `OrderRepositoryTest` fixture can traverse product images.

- [ ] **Step 2: Add a failing association traversal test**

In `ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java`, add this test:

```java
@Test
void 주문상품에서_SKU_상품_대표이미지까지_LAZY_연관경로를_탐색한다() {
    List<Orders> orders = orderRepository.findByUserId(savedUserId);
    assertThat(orders).hasSize(3);

    OrderItem firstItem = orders.getFirst().getOrderItems().getFirst();

    assertThat(firstItem.getProductSku().getId()).isEqualTo(100L);
    assertThat(firstItem.getProductSku().getProduct().getId()).isEqualTo(100L);
    assertThat(firstItem.getProductSku().getProduct().getImages())
            .extracting(ProductImage::getImageUrl)
            .contains("https://example.com/product-100-main.jpg");
}
```

Add imports:

```java
import com.dblab.ecommerce.entity.OrderItem;
import com.dblab.ecommerce.entity.ProductImage;
```

- [ ] **Step 3: Run the focused test and verify it fails**

Run:

```bash
cd ecommerce && rtk gradlew test --tests com.dblab.ecommerce.repository.OrderRepositoryTest
```

Expected: compilation fails because `Orders.getOrderItems()`, `OrderItem.getProductSku()`, `ProductSku.getProduct()`, or `Product.getImages()` does not exist.

- [ ] **Step 4: Add lazy associations**

Modify entity classes with read-only associations while preserving scalar FK fields.

`Orders.java`:

```java
import java.util.ArrayList;
import java.util.List;

@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
@OrderBy("id ASC")
@Builder.Default
private List<OrderItem> orderItems = new ArrayList<>();
```

`OrderItem.java`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id", insertable = false, updatable = false)
private Orders order;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "sku_id", insertable = false, updatable = false)
private ProductSku productSku;
```

`ProductSku.java`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", insertable = false, updatable = false)
private Product product;
```

`Product.java`:

```java
import java.util.ArrayList;
import java.util.List;

@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
@OrderBy("isMain DESC, sortOrder ASC, id ASC")
@Builder.Default
private List<ProductImage> images = new ArrayList<>();
```

`ProductImage.java`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", insertable = false, updatable = false)
private Product product;
```

Expected: scalar FK fields remain the insert/update source; associations are lazy read paths.

- [ ] **Step 5: Extend `OrderResponse` thumbnail contract**

Modify `OrderResponse.OrderItemDto` to include `thumbnailUrl`.

Use this record shape:

```java
public record OrderItemDto(
        Long itemId,
        String productName,
        Integer quantity,
        Integer unitPrice,
        String thumbnailUrl) {
    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                findThumbnailUrl(item));
    }

    private static String findThumbnailUrl(OrderItem item) {
        if (item.getProductSku() == null || item.getProductSku().getProduct() == null) {
            return null;
        }
        return item.getProductSku().getProduct().getImages().stream()
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }
}
```

Add import:

```java
import com.dblab.ecommerce.entity.ProductImage;
```

- [ ] **Step 6: Run the focused test and verify it passes**

Run:

```bash
cd ecommerce && rtk gradlew test --tests com.dblab.ecommerce.repository.OrderRepositoryTest
```

Expected: `OrderRepositoryTest` passes.

- [ ] **Step 7: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/entity ecommerce/src/main/java/com/dblab/ecommerce/dto/OrderResponse.java ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java ecommerce/src/test/resources/test-data/order-test-setup.sql
git commit -m "feat: add phase 3 lazy order thumbnail associations"
```
