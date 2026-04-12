package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상품 SKU (Stock Keeping Unit)
 * - 옵션 조합마다 재고와 추가 금액을 별도 관리
 * - stock_quantity >= 0 CHECK 제약 — Phase 4 격리 수준 실험 전제조건
 */
@Entity
@Table(name = "product_sku")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, unique = true)
    private String skuCode;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private Integer extraPrice;
}
