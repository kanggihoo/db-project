package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/** SKU ↔ 옵션값 연결 중간 테이블 */
@Entity
@Table(name = "product_sku_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductSkuOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long skuId;

    @Column(nullable = false)
    private Long optionValueId;
}
