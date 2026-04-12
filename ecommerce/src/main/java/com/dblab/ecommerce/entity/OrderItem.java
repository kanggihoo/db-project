package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 주문 항목 엔티티
 * - product_name, option_info: 주문 시점 스냅샷 저장
 *   (이후 상품 정보 변경과 무관하게 주문 당시 정보 보존)
 */
@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long skuId;

    @Column(nullable = false)
    private String productName;

    private String optionInfo;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private String status;
}
