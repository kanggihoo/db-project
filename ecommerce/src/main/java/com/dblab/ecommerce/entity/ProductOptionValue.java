package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/** 상품 옵션 실제 값 (빨강, M 등) */
@Entity
@Table(name = "product_option_value")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductOptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long optionId;

    @Column(nullable = false)
    private String value;
}
