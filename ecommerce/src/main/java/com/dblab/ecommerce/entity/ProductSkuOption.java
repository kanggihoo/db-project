package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * SKU ↔ 옵션값 연결 중간 테이블 (Mapping Table)
 * - 특정 SKU가 어떤 옵션값들(빨강, XL 등)의 조합으로 구성되었는지 연결합니다.
 * - ProductSku : ProductOptionValue = N : M 관계를 해소하기 위한 다리 역할입니다.
 */
@Entity
@Table(name = "product_sku_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductSkuOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 매핑 식별 PK

    @Column(nullable = false)
    private Long skuId; // SKU 식별 번호 (ProductSku 테이블 FK)

    @Column(nullable = false)
    private Long optionValueId; // 옵션값 식별 번호 (ProductOptionValue 테이블 FK)
}

