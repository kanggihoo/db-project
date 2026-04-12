package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/** 상품 이미지 */
@Entity
@Table(name = "product_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isMain;

    @Column(nullable = false)
    private Integer sortOrder;
}
