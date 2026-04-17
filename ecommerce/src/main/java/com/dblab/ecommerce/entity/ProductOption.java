package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상품 옵션 종류 엔티티
 * - 상품이 어떤 종류의 옵션을 가질 수 있는지 정의합니다 (예: 색상, 사이즈 등).
 * - Product : ProductOption = 1 : N 관계
 */
@Entity
@Table(name = "product_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 옵션 식별 PK

    @Column(nullable = false)
    private Long productId; // 상품 식별 번호 (Product 테이블 FK)

    @Column(nullable = false)
    private String optionName; // 옵션 명칭 (예: "색상", "사이즈")
}

