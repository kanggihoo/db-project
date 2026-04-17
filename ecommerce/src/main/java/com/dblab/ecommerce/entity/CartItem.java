package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 장바구니 품목 엔티티 (Detail)
 * - 장바구니에 담긴 개별 상품 조각(SKU)들을 관리합니다.
 * - 구매 확정 전이므로 정보를 박제하지 않고, skuId를 통해 매번 최신 상품 정보를 조회합니다.
 */
@Entity
@Table(name = "cart_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 장바구니 품목 식별 PK

    @Column(nullable = false)
    private Long cartId; // 연관된 장바구니 식별 번호 (Cart 테이블 FK)

    @Column(nullable = false)
    private Long skuId; // 장바구니에 담긴 상품 SKU 식별 번호 (ProductSku 테이블 FK)

    @Column(nullable = false)
    private Integer quantity; // 사용자가 장바구니에 담은 수량

    @Column(nullable = false)
    private LocalDateTime addedAt; // 장바구니에 담은 시각
}

