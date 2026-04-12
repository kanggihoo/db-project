package com.dblab.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 주문 엔티티
 * - status 분포: DELIVERED(50%), SHIPPED(15%), PREPARING(10%), PAID(10%), PENDING(10%), CANCELLED(5%)
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
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long addressId;

    // nullable: 쿠폰 미사용 주문
    private Long usedCouponId;

    @Column(nullable = false)
    private Integer totalPrice;

    @Column(nullable = false)
    private Integer discountPrice;

    @Column(nullable = false)
    private Integer finalPrice;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum Status { PENDING, PAID, PREPARING, SHIPPED, DELIVERED, CANCELLED }
}
