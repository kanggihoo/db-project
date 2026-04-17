package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 주문 마스터(Header) 엔티티
 * - 누가, 어디로, 얼마에 주문했는지에 대한 주문 전체의 요약 정보를 관리합니다.
 * - 개별 상품 내역과 수량은 세부 테이블인 OrderItem에서 관리합니다.
 * - status 분포: DELIVERED(50%), SHIPPED(15%), PREPARING(10%), PAID(10%),
 * PENDING(10%), CANCELLED(5%)
 * - used_coupon_id: nullable (전체 주문의 30%만 쿠폰 적용)
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 주문 식별 PK

    @Column(nullable = false)
    private Long userId; // 주문자 식별 번호 (Users 테이블 FK)

    @Column(nullable = false)
    private Long addressId; // 배송지 식별 번호 (UserAddress 테이블 FK)

    // nullable: 쿠폰 미사용 주문
    private Long usedCouponId; // 사용된 쿠폰 식별 번호 (UserCoupon 테이블 FK, 미사용 시 null)

    @Column(nullable = false)
    private Integer totalPrice; // 할인 적용 전 총 상품 금액 합계

    @Column(nullable = false)
    private Integer discountPrice; // 쿠폰 등을 통한 총 할인 금액

    @Column(nullable = false)
    private Integer finalPrice; // 사용자가 실제로 결제해야 할 최종 금액

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status; // 주문 상태 (결제대기, 결제완료, 배송중 등)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 주문 일시

    public enum Status {
        PENDING, PAID, PREPARING, SHIPPED, DELIVERED, CANCELLED
    }
}
