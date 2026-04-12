package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/** 상품 옵션 종류 (색상, 사이즈 등) */
@Entity
@Table(name = "product_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String optionName;
}
