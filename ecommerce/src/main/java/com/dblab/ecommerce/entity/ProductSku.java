package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상품 SKU (Stock Keeping Unit) 엔티티
 * - 상품의 실제 판매 및 재고 관리 단위를 의미합니다.
 * - 옵션들의 특정 조합마다 하나의 SKU가 생성되어 재고와 가격을 별도로 관리합니다.
 * - Product : ProductSku = 1 : N 관계
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
    private Long id; // SKU 식별 PK

    @Column(nullable = false)
    private Long productId; // 상품 식별 번호 (Product 테이블 FK)

    @Column(nullable = false, unique = true)
    private String skuCode; // SKU 고유 코드 (재고 식별용)

    @Column(nullable = false)
    private Integer stockQuantity; // 현재 재고 수량 (0 이상 필수)

    @Column(nullable = false)
    private Integer extraPrice; // 해당 옵션 조합 선택 시 추가되는 금액
}

