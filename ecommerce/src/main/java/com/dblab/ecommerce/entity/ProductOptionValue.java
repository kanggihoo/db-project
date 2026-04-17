package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상품 옵션 실제 값 엔티티
 * - 특정 옵션 종류에 속하는 실제 값들을 저장합니다 (예: "빨강", "파랑", "M", "L").
 * - ProductOption : ProductOptionValue = 1 : N 관계
 */
@Entity
@Table(name = "product_option_value")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductOptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 옵션값 식별 PK

    @Column(nullable = false)
    private Long optionId; // 옵션 종류 식별 번호 (ProductOption 테이블 FK)

    @Column(nullable = false)
    private String value; // 실제 옵션값 (예: "빨강", "XL")
}

