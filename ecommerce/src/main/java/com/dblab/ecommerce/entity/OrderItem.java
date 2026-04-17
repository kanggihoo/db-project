package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 주문 상세 항목 엔티티 (Detail)
 * - 하나의 주문(Orders)에 포함된 개별 상품들의 정보를 관리합니다.
 * - 주문 시점의 상품명, 옵션, 가격을 스냅샷 형태로 저장하여 데이터 불변성을 보존합니다.
 * - Orders : OrderItem = 1 : N 관계 (OrderItem이 N 측)
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
    private Long id; // 주문 항목 식별 PK

    @Column(nullable = false)
    private Long orderId; // 해당 주문의 식별 번호 (Orders 테이블 FK)

    @Column(nullable = false)
    private Long skuId; // 주문한 상품 SKU 식별 번호 (ProductSku 테이블 FK)

    @Column(nullable = false)
    private String productName; // 주문 당시의 상품명 (정보 보존을 위한 스냅샷)

    private String optionInfo; // 주문 당시의 옵션 정보 (예: "색상: 빨강 / 사이즈: XL")

    @Column(nullable = false)
    private Integer quantity; // 주문 수량

    @Column(nullable = false)
    private Integer unitPrice; // 주문 당시의 단가 (할인 전 금액)

    @Column(nullable = false)
    private String status; // 개별 상품의 상태 (배송준비, 취소 등)
}

